## Description
A socket server on top of Weka.jar enabling efficient classification of test data using any programming language capable of acting as a socket client

## Run the server using the executable jar

```
#!bash

java -jar .\socketweka.jar 9100
```


## Communication format

* First message sent to server must contain the pre-trained .model file path
	A reply from the server will specifies success or failure.

* Requests must be in the following format. Containing all the features in a json array:

```
#!json

{"attributes":[-0.020187378,0.033355713,-0.06762695,-0.008514404,0.0035552979,0.8539276]}
```


* Reply will have the following json format:

```
#!json

{"index":1,"dist":[1.4402003129542426E-10,0.9991183106175616,4.386999171079445E-4,4.4298932131057593E-4],"label":"Idle"}
```