#include <SoftwareSerial.h>


int bluetoothTx = 2;
int bluetoothRx = 3;

int ledPin = 13;
int relayPin = 12;

SoftwareSerial bluetooth(bluetoothTx, bluetoothRx);

void setup() {
    Serial.println("beginsetup");
    Serial.begin(9600);
    bluetooth.begin(115200);
    bluetooth.print("$$$");
    delay(100);
    bluetooth.println("U,9600,N");
    bluetooth.begin(9600);
    pinMode(ledPin, OUTPUT);
    pinMode(relayPin, OUTPUT);
    Serial.println("endsetup");
}

void loop() {
    if (bluetooth.available())
    {
        char command = bluetooth.read();
        Serial.println("received " + command);
        // enabling relay requires setting pin to low
	if (command == '1') {
            digitalWrite(relayPin, LOW);
       	    flashSignal();
        }
        else if (command == '0') {
            digitalWrite(relayPin, HIGH);
            flashSignal();
        }
        int returnState = digitalRead(relayPin);
        Serial.println("sending " + returnState);
	bluetooth.write(returnState);
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
