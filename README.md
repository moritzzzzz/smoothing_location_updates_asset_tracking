# Smoothing of location updates for good user experience in asset tracking
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

