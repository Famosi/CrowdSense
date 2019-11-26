# CrowdSense
This is an Android crowdsensing app developed for academic purpose.

## About the project
The homepage of the app provides a list of available tasks. 

Each task consists of detecting data from the environment through the use of sensors' phone.

Each task is a JSON object with the following format:
```       
{
  "ID" : "LookForKoalas" "issuer" : "LAM_UNIBO_2018", "type" : "picture",
  "lat" : "-37.835309",
  "lon" : "145.047363", "radius" : "1.0",
  "duration" : "5",
  "what" : "eucalyptus_trees"
}
```

• **ID** [string] is an arbitrary string for uniquely distinguish the task.

• **issuer** [string] the Twitter account of the issuer.

• **type** [string] it’s either “picture” (the user has to take a picture of the object), “light” (the user has to use the light sensor), “noise” (the user has to record the dB value of the microphone), “temperature” (the user has to record the temperature value in Celsius) and “RSSI” (the user has to send the signal strength of the cellular technology).

• **lat** [float] Latitude of the task center.

• **lon** [float] Longitude of the task center.

• **radius** [float] radius (in meters) of the task (thus, the task can be per- formed within a circular area).

• **duration** [int] How long is the task valid since it has been posted (in days).

• **what** [string] what are we interested in, this is an arbitrary string, it only contains information on what we are aiming to observe.

The tasks, issued as twitter posts containing the hashtag #LAM_CROWD18 and the JSON string, are taken using the [Named Link](https://developer.twitter.com/en/docs "Twitter API"). 

The application notifies whenever a new task is being posted and the user has to manually accept it (or reject it). Once the task has been accepted it is saved locally until it is performed. Whenever the conditions for performing the task are met (the user is in the correct area within the task duration) the user is notified about the chance of performing the task.


