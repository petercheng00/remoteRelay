#include <SoftwareSerial.h>


int bluetoothTx = 2;
int bluetoothRx = 3;

int ledPin = 13;
int relayPin = 12;

SoftwareSerial bluetooth(bluetoothTx, bluetoothRx);

void setup() {
    Serial.begin(9600);
    bluetooth.begin(115200);
    bluetooth.print("$$$");
    delay(100);
    bluetooth.println("U,9600,N");
    bluetooth.begin(9600);
    
    pinMode(ledPin, OUTPUT);
    pinMode(relayPin, OUTPUT);
    digitalWrite(relayPin, HIGH);
}

void loop() {
    if (bluetooth.available())
    {
        Serial.println("available");
        char command = bluetooth.read();
        Serial.println("received");
        Serial.println(command);
        // enabling relay requires setting pin to low
	if (command == '1') {
            Serial.println("turning on");
            digitalWrite(relayPin, LOW);
       	    flashSignal();
        }
        else if (command == '0') {
            Serial.println("turning off");
            digitalWrite(relayPin, HIGH);
            flashSignal();
        }
        char returnState = '0';
        if (digitalRead(relayPin) == LOW) {
            returnState = '1';
        }
        Serial.println("sending");
        Serial.println(returnState);
	bluetooth.print(returnState);
    }
}
void flashSignal() {
  for (int i = 0; i < 4; ++i)
  {
    digitalWrite(ledPin, HIGH);
    delayMicroseconds(500);
    digitalWrite(ledPin,LOW);
    delayMicroseconds(250);
  }
}
