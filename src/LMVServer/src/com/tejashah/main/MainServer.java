package com.tejashah.main;

import java.io.IOException;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.pi4j.wiringpi.SoftPwm;

/**
 * @author Tejas Shah
 * @version Nov 8, 2015
 */
public class MainServer {
	private static Firebase base = new Firebase("https://lmv-project.firebaseio.com/");
	private static final int LEFT_MOTOR_PIN_NUMBER = 1;
	private static final int RIGHT_MOTOR_PIN_NUMBER = 24;
	private static final int DEFAULT_CYCLE_LENGTH = 8;
	
	public static class Motor {
		private int pinNumber;
		private boolean isRunning = false;
		private boolean isModding = false;
		
		private int speed = 0;
		
		//default = neutral = 1.5 ms
		long activePauseMS = 1;
		int  activePauseNS = 500 * 1000;
		int  passivePauseNS = 1000 * 1000 - activePauseNS;
		long passivePauseMS = DEFAULT_CYCLE_LENGTH - activePauseMS - (activePauseNS != 0 ? 1 : 0);
		
		private Thread thread = new Thread() {
			final Object lock = new Object();
			
			private void sleepFor(long ms, int nano) {
				try {
					sleep(ms, nano);
				} catch (InterruptedException ex) {
					ex.printStackTrace();
				}
			}
			
			private synchronized void doCycle(int pinNum, long ams, int ans, long pms, int pns) {
				SoftPwm.softPwmWrite(pinNum, 100);
				sleepFor(ams, ans);
				SoftPwm.softPwmWrite(pinNum, 0);
				sleepFor(pms, pns);
			}
			
			@Override
			public void run() {
				while (isRunning) {
					if (!isModding) {
						synchronized (lock) {
							doCycle(pinNumber, activePauseMS, activePauseNS, passivePauseMS, passivePauseNS);
						}
					}
				}
			}
		};
		
		public Motor(int pinNumber) {
			this.pinNumber = pinNumber;
			SoftPwm.softPwmCreate(pinNumber, 0, 100);
		}
		
		public void start() {
			isRunning = true;
			thread.start();
		}
		
		public void stop() {
			isRunning = false;
		}
		
		public void setSpeed(int value) {
			isModding = true;
			
			if (value < -127)
				value = -127;
			
			if (value > 127)
				value = 127;
			
			speed = value;
			
			float newPause = ((value / 127f) / 2f) + 1.5f;
			long milli = (long) newPause;
			int nano = ((int) ((newPause - milli) * 1000)) * 1000;
			
			activePauseMS = milli;
			activePauseNS = nano;
			passivePauseNS = 1000 * 1000 - activePauseNS;
			passivePauseMS = DEFAULT_CYCLE_LENGTH - activePauseMS - (activePauseNS != 0 ? 1 : 0);
			
			isModding = false;
		}
		
		public int getSpeed() {
			return speed;
		}
	}
	
	public static abstract class BetterValueEventListener implements ValueEventListener {
		@Override public void onCancelled(FirebaseError error) {}
		
		protected final Motor leftMotor = new Motor(LEFT_MOTOR_PIN_NUMBER);
		protected final Motor rightMotor = new Motor(RIGHT_MOTOR_PIN_NUMBER);

		public void startMotors() {
			leftMotor.start();
			rightMotor.start();
		}
		
		public void stopMotors() {
			leftMotor.stop();
			rightMotor.stop();
		}
		
		private boolean inRange(float num, float min, float max) {
			return num >= min && num <= max;
		}
		
		protected int zValueToSpeed(float z) {
			int value;
			
			if (z > 90)
				value = -127;
			else if (inRange(z, 30, 90))
				value = -63;
			else if (inRange(z, -30, 30))
				value = 0;
			else if (inRange(z, -90, -30))
				value = 63;
			else
				value = 127;
			
			return value;
		}
	}
	
	static BetterValueEventListener listener = new BetterValueEventListener() {
		@Override
		public void onDataChange(DataSnapshot snapshot) {
			String rawValues = snapshot.getValue(String.class);
			String[] rawValueParts = rawValues.split("/");
			
			float leftMotorZ = Float.parseFloat(rawValueParts[0]);
			float rightMotorZ = Float.parseFloat(rawValueParts[1]);
			
			int leftSpeed = zValueToSpeed(leftMotorZ);
			int rightSpeed = zValueToSpeed(rightMotorZ);
			
			System.out.println(leftSpeed + "/" + rightSpeed);
			
			if (leftSpeed != leftMotor.getSpeed()) {
				leftMotor.setSpeed(leftSpeed);
			}
			
			if (rightSpeed != rightMotor.getSpeed()) {
				rightMotor.setSpeed(rightSpeed);
			}
			
			//System.out.println(leftMotorZ + "," + rightMotorZ);
		}
	};
	
	public static void main(String[] args) {
		listener.startMotors();
		
		base.child("motorValues").addValueEventListener(listener);
		
		// Keep this process running until Enter is pressed
		System.out.println("Press Enter to quit...");
		try {
			System.in.read();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		listener.stopMotors();
		base.child("motorValues").removeEventListener(listener);
	}
}