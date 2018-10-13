package com.tejashah.main;

import java.io.IOException;

import com.firebase.client.Firebase;
import com.leapmotion.leap.Controller;
import com.leapmotion.leap.Frame;
import com.leapmotion.leap.Hand;
import com.leapmotion.leap.Listener;
import com.leapmotion.leap.Vector;

/**
 * @author Tejas Shah
 * @version Nov 8, 2015
 */
public class MainClient {
	static Firebase base = new Firebase("https://lmv-project.firebaseio.com/");
	
	static Listener pivexListener = new Listener() {
		int frameCount = 0;
		final int frameSkip = 10;
		
		@Override
		public void onInit(Controller controller) {
			System.out.println("Leap Motion: Initialized!");
		}
		
		@Override
		public void onConnect(Controller controller) {
			System.out.println("Leap Motion: Connected");
		}
		
		@Override
		public void onDisconnect(Controller controller) {
			// Note: not dispatched when running in a debugger.
			System.out.println("Leap Motion: Disconnected");
		}
		
		@Override
		public void onExit(Controller controller) {
			System.out.println("Leap Motion: Exited");
		}
		
		@Override
		public void onFrame(Controller controller) {
			Frame frame = controller.frame();
			
			if (frame.hands().count() != 2) {
				return;
			}
			
			frameCount++;
			//System.out.println(frameCount);
			if (frameCount % frameSkip != 0) {
				return;
			}
			
			//at this point there are only 2 hands
			
			Hand leftHand = null, rightHand = null;
			
			for (Hand hand : frame.hands()) {
				if (hand.isRight())
					rightHand = hand;
				else //if 2 right or 2 left hands are there, it dont matter
					leftHand = hand;
			}
			
			if (leftHand == null || rightHand == null) {
				System.out.println("alert");
				return;
			}
			
			Vector leftPalmCenter = leftHand.palmPosition();
			Vector rightPalmCenter = rightHand.palmPosition();
			
			System.out.println(leftPalmCenter.getZ() + "/" + rightPalmCenter.getZ());
			base.child("motorValues").setValue(leftPalmCenter.getZ() + "/" + rightPalmCenter.getZ());
		}
	};
	
	public static void main(String[] args) {
		Controller controller = new Controller();
		
		// Have the sample listener receive events from the controller
		controller.addListener(pivexListener);
		
		// Keep this process running until Enter is pressed
		System.out.println("Press Enter to quit...");
		try {
			System.in.read();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// Remove the sample listener when done
		controller.removeListener(pivexListener);
	}
	
}
