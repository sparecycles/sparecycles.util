package com.github.sparecycles.util.input;

import java.util.Collection;
import java.util.HashMap;
import java.util.Stack;

import android.view.MotionEvent;
import android.view.View;

public abstract class Touch<FingerType extends Touch.Finger>
	implements View.OnTouchListener
{
	private final HashMap<Number, FingerType> fingers = new HashMap<Number, FingerType>();
	
	public Collection<FingerType> values()
	{
		return fingers.values();
	}
	
	public static interface Finger
	{
		public void cancel();
		public void touch(int x, int y, float pressure);
		public void release(int x, int y, float pressure);
		public void move(int x, int y, float pressure);
	};
	
	public abstract FingerType poke();
	public abstract void recycle(FingerType finger);
	
	public static abstract class Multi<MultiFingerType extends Touch.Finger> extends Touch<MultiFingerType>
	{
		Stack<MultiFingerType> pointers = new Stack<MultiFingerType>();
		
		protected abstract MultiFingerType make();
		
		public MultiFingerType poke()
		{
			MultiFingerType pointer = !pointers.empty() ? pointers.pop() : make();
			return pointer;
		}

		public void recycle(MultiFingerType finger)
		{
			pointers.push(finger);				
		}
	}

	public static abstract class Single<SingleFingerType extends Touch.Finger> extends Touch<SingleFingerType>
	{
		SingleFingerType finger;
		boolean active = false;
		
		public Single(SingleFingerType finger)
		{
			this.finger = finger;
		}
				
		public SingleFingerType poke()
		{
			if(active)
				return null;

			active = true;
			return finger;
		}

		public void recycle(SingleFingerType finger)
		{
			active = false;	
		}
	}
	
	public void feel(View view)
	{
		view.setOnTouchListener(this);		
	}

	public boolean onTouch(View view, MotionEvent event)
	{
		int fingerIndex = event.getActionIndex();
		int fingerId = event.getPointerId(fingerIndex);
		int action = event.getActionMasked();
		int x, y;
		float pressure;
		FingerType finger;

		switch(action) {
		case MotionEvent.ACTION_DOWN:
		case MotionEvent.ACTION_POINTER_DOWN:
			x = (int)event.getX(fingerIndex);
			y = (int)event.getY(fingerIndex);
			pressure = event.getPressure(fingerIndex);

			FingerType newFinger = poke();
			if(newFinger == null)
				break;
			fingers.put(fingerId, newFinger);
			newFinger.touch(x, y, pressure);
			newFinger.move(x, y, pressure);
			break;
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_POINTER_UP:
			if(!fingers.containsKey(fingerId))
				break;;
			finger = fingers.remove(fingerId);
			x = (int)event.getX(fingerIndex);
			y = (int)event.getY(fingerIndex);
			pressure = event.getPressure(fingerIndex);
			finger.release(x, y, pressure);
			recycle(finger);
			break;
		case MotionEvent.ACTION_OUTSIDE:
		case MotionEvent.ACTION_CANCEL:
			if(!fingers.containsKey(fingerId))
				break;
			finger = fingers.remove(fingerId);
			finger.cancel();
			recycle(finger);
			break;
		case MotionEvent.ACTION_MOVE:
			int historySize = event.getHistorySize();
			int fingerCount = event.getPointerCount();
			for(fingerIndex = 0; fingerIndex < fingerCount; fingerIndex++)
			{
				fingerId = event.getPointerId(fingerIndex);
				if(!fingers.containsKey(fingerId))
					continue;
			
				finger = fingers.get(fingerId);
				
				for(int pos = 0; pos < historySize; pos++)
				{
					x = (int)event.getHistoricalX(fingerIndex, pos);
					y = (int)event.getHistoricalY(fingerIndex, pos);
					pressure = event.getHistoricalPressure(fingerIndex, pos);
					finger.move(x, y, pressure);
				}
				x = (int)event.getX(fingerIndex);
				y = (int)event.getY(fingerIndex);
				pressure = event.getPressure(fingerIndex);
				finger.move(x, y, pressure);
			}
			
			break;
		}
		return true;
	}
}