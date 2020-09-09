package swim.sattrack.agents;

import swim.api.SwimLane;
import swim.api.agent.AbstractAgent;
import swim.api.lane.CommandLane;
import swim.api.lane.ValueLane;
import swim.api.lane.MapLane;
import swim.structure.Value;
import swim.structure.Record;
import swim.uri.Uri;

public class SatelliteAgent extends AbstractAgent {

  final private int HISTORY_SIZE = 100;

  @SwimLane("catalogNumber")
  protected ValueLane<Value> catalogNumber;

  @SwimLane("name")
  protected ValueLane<Value> name;

  @SwimLane("latitude")
  protected ValueLane<Value> latitude;

  @SwimLane("longitude")
  protected ValueLane<Value> longitude;

  @SwimLane("fullRowData")
  protected ValueLane<Value> fullRowData;

  @SwimLane("tracks")
  protected MapLane<Long, Value> tracks = this.<Long, Value>mapLane()
    .didUpdate((key, newValue, oldValue) -> {
      if (this.tracks.size() > HISTORY_SIZE) {
        this.tracks.remove(this.tracks.getIndex(0).getKey());
      }
    });

  @SwimLane("tle")
  protected ValueLane<Value> tle;  

  /**
    Value Lane which holds the last update timestamp
   */
  @SwimLane("lastUpdate")
  protected ValueLane<Long> lastUpdate = this.<Long>valueLane();  

  @SwimLane("updateData")
  public CommandLane<Value> updateData = this.<Value>commandLane()
      .onCommand((Value newValue) -> {
        if(!newValue.equals(Value.absent())) {
          this.updateSatellite(newValue);
        }
      });      
    
  private void updateSatellite(Value stateData) {
    long timestamp = System.currentTimeMillis();
    // Value stateData = newValue;//Json.parse(newValue.stringValue());  // convert incoming value into JSON

    this.fullRowData.set(stateData); // store new state data on fullState Value Lane
    this.catalogNumber.set(stateData.get("catalogNumber"));
    this.name.set(stateData.get("name"));
    this.latitude.set(stateData.get("latitude"));
    this.longitude.set(stateData.get("longitude"));
    this.tle.set(stateData.get("tle"));

    // System.out.println(this.tle.get());

    // Record currentTrackPoint = Record.create(2)
    // .slot("lat", this.latitude.get().floatValue(0f))
    // .slot("lng", this.longitude.get().floatValue(0f));

    Value tracks = stateData.get("tracks");

    if(!tracks.equals(Value.absent())) {
      tracks.forEach(trackPoint -> {

        if(trackPoint != Value.absent()) {
          Value currentTrackPoint = Record.create(2)
          .slot("lat", trackPoint.get("lat").floatValue(0f))
          .slot("lng", trackPoint.get("long").floatValue(0f))
          .toValue();
        
          // System.out.println(String.format("%s %s", trackPoint.get("timestamp").longValue(0l), currentTrackPoint));
          this.tracks.put(trackPoint.get("timestamp").longValue(0l), currentTrackPoint);    
        }
      });
  
    }
    
    

    Record shortInfo = Record.create()
      .slot("name", stateData.get("name"))
      .slot("catalogNumber", stateData.get("catalogNumber"))
      .slot("intlDesignator", stateData.get("intlDesignator"))
      .slot("type", stateData.get("type"))
      .slot("orbitalPeriod", stateData.get("orbitalPeriod"))
      .slot("tle", stateData.get("tle"))
      .slot("velocity", stateData.get("velocity"))
      .slot("height", stateData.get("height"))
      .slot("latitude", stateData.get("latitude"))
      .slot("longitude", stateData.get("longitude"));

    command(Uri.parse("warp://127.0.0.1:9001"), Uri.parse("aggregation"), Uri.parse("addSatellite"), shortInfo); 

    this.lastUpdate.set(timestamp); // update lastUpdate Value Lane

    if(this.name.get().equals(Value.absent()) && !stateData.equals(Value.absent())) {

      System.out.println("[SatelliteAgent] new satellite " + stateData.get("name").stringValue());
  
    }

    

  }
  /**
    Standard startup method called automatically when WebAgent is created
   */
  @Override
  public void didStart() {

  }

}