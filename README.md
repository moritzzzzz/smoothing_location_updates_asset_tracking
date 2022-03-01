# Smoothing of location updates in asset tracking
## Problem
The problem many asset trackers are facing is, that the position updates of the asset are delayed due to instable mobile network connection. 
This either leads to missing location updates, or to batch updates.

Both cases create a uncomfortable user experience. 

### Missing location updates
When location updates are missing, the asset position indicator would jump to the next position with an unrealistic speed.

### Batch location updates
If location updates come in as a batch, the asset position indicator will also jump to the latest position and leave out the interim location updates.

## Solution
If the geographical accuracy of the asset position indicator is less important than the realistic and continuous movement of the position indicator,
the location updates can be smoothed client side. This client side smoothing is what I have implemented in this example by using 2 techniques:

- A location update buffer
- A location update distance dependent position indicator animation duration 

![smooth](https://user-images.githubusercontent.com/43645757/153377103-42178adf-2843-4c14-b171-4fa1ea5cac54.gif)


### Location buffer
A location buffer queue is deployed that is fed with location updates of the position indicator. This buffer queue passes out the next update event only
 after the distance dependent animation has finished. 
 
 This fixes the problem of batch location updates that result in jumpiness.

### Distance dependent animation duration
A location update distance dependent animation duration is introduced. This introduces a constant speed of the position indicator on the map.
This should be set to the true average speed of the asset.
If the position indicator is behind (meaning that there are events queuing in the buffer) the position indicators speed is slightly increased so that it catches up
 with the true location of the asset. 
 Once it has fetched up, the movement speed is reduced to the average speed again.
 
 This fixes unrealistic position jumps, even when the position indicator is behind the true location of the asset. (it will use a slightly faster, but still realistic speed to catch up with the asset)

## Usage
This example implements the above 2 techniques. It is listening for mapClick events. Each such event is regarded as a location update event and is pushed into the location buffer.

This example uses exaggerated speed values to better display the impact. 

These speed values are in m/s and should be adjusted to realistic values:
```
    private var asset_speed_fast = 100
    private var asset_speed_normal = 60
```

### Add you Mapbox Access Token
Your Mapbox Access Token would have to be added to `strings.xml`

