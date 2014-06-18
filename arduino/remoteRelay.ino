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
}

void loop() {
    if (bluetooth.available())
    {
        command = bluetooth.readStringUntil('\n');
	if (command == '1') {
            digitalWrite(relayPin, HIGH);
        }
        else {
            digitalWrite(relayPin, LOW);
        }
	flashSignal();
	bluetooth.write(digitalRead(relayPin))
    }
}
void flashSignal() {
  for (int i = 0; i < 4; ++i)
  {
    digitalWrite(signalLedPin, HIGH);
    delayMicroseconds(500);
    digitalWrite(signalLedPin,LOW);
    delayMicroseconds(250);
  }
}
