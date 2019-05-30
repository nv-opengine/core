package com.gracefulcode;

import com.gracefulcode.opengine.v2.NDimensional;

import java.util.ArrayList;
import java.util.Random;

public class TestApp {
	public static class Person {
		public NDimensional personality = new NDimensional(2);
		public ArrayList<Integer> friends = new ArrayList<Integer>();

		public void populateFriends(Random r, int max, int numFriends) {
			for (int i = 0; i < numFriends; i++) {
				int friend = r.nextInt(max);
				this.friends.add(friend);
			}
		}

		public void adjustFriends(Random r, ArrayList<Person> people) {
			for (int i = 0; i < friends.size(); i++) {
				Person other = people.get(i);
				float dst2 = this.personality.dst2(other.personality);
				if (dst2 > 0.5f || dst2 == 0.0f) {
					for (int z = 0; z < 10000; z++) {
						int newFriend = r.nextInt(people.size());
						if (newFriend == i) continue;

						other = people.get(newFriend);
						dst2 = this.personality.dst2(other.personality);
						if (dst2 < r.nextFloat()) {
							this.friends.set(i, newFriend);
							break;
						}
					}
				}
			}
		}

		public void randomizePersonality(Random r) {
			this.personality = new NDimensional(2);
			this.personality.set(0, r.nextFloat() * 2 - 1.0f);
			this.personality.set(1, r.nextFloat() * 2 - 1.0f);
		}

		public void adjust(ArrayList<Person> people) {
			NDimensional friendShift = new NDimensional(2);
			NDimensional otherShift = new NDimensional(2);

			for (int i = 0; i < people.size(); i++) {
				Person person = people.get(i);
				float dst2 = person.personality.dst2(this.personality);

				if (dst2 > 0.1f) {
					NDimensional adjust = this.personality.cpy();
					adjust.mul(-1);
					adjust.add(person.personality);

					// adjust.mul((float)Math.sqrt(dst2));

					// adjust.mul(1.0f / dst2);
					// if (adjust.length() < 0.1f) {
					// 	adjust.mul(1.0f / dst2);
					// 	adjust.mul(-1);
					// }

					if (this.friends.contains(i)) {
						friendShift.add(adjust);
					} else {
						adjust.mul(1.0f / dst2);
						adjust.mul(-1);
						otherShift.add(adjust);
					}
				}
			}

			friendShift.normalize();
			otherShift.normalize();

			friendShift.add(otherShift);
			friendShift.normalize();
			friendShift.mul(0.01f);
			this.personality.add(friendShift);
			// this.personality.normalize();
		}
	}

	protected static ArrayList<Person> a = new ArrayList<Person>();
	protected static ArrayList<Person> b = new ArrayList<Person>();

	public static void main(String[] args) {
		Random r = new Random();
		int iterations = 3000;
		int div = iterations / 25;
		float threshold = 0.25f;
		int overthreshold = 0;
		int underthreshold = 0;
		int numPeople = 150;

		/**
		 * Fill world. a is real data. b is dummy.
		 */
		for (int i = 0; i < numPeople; i++) {
			Person p = new Person();
			p.randomizePersonality(r);
			p.populateFriends(r, numPeople, 5);
			TestApp.a.add(p);

			Person p2 = new Person();
			p2.personality = p.personality.cpy();
			p2.friends = p.friends;
			TestApp.b.add(p2);
		}

		int displayIteration = 0;

		for (int it = 0; it < iterations; it++) {
			for (int i = 0; i < a.size(); i++) {
				TestApp.a.get(i).adjust(TestApp.b);
			}
			for (int i = 0; i < a.size(); i++) {
				TestApp.b.get(i).adjust(TestApp.a);
			}

			float maxValue = 0.0f;
			for (int i = 0; i < a.size(); i++) {
				Person p = TestApp.a.get(i);
				if (Math.abs(p.personality.get(0)) > maxValue) maxValue = Math.abs(p.personality.get(0));
				if (Math.abs(p.personality.get(1)) > maxValue) maxValue = Math.abs(p.personality.get(1));
			}

			for (int i = 0; i < a.size(); i++) {
				Person p = TestApp.a.get(i);
				p.personality.mul(1.0f / maxValue);
				p = TestApp.b.get(i);
				p.personality.mul(1.0f / maxValue);
			}

			if (it % div == 0) {
				for (int i = 0; i < a.size(); i++) {
					int yOffset = (int)Math.floor(displayIteration / 5);
					int xOffset = displayIteration - (yOffset * 5);

					Person person = TestApp.a.get(i);
					System.out.println("(" + (person.personality.get(0) + (5.0f * xOffset)) + "," + (person.personality.get(1) - (5.0f * yOffset)) + ")");
				}

				displayIteration++;

				for (int i = 0; i < TestApp.a.size(); i++) {
					Person p = TestApp.a.get(i);
					p.adjustFriends(r, TestApp.a);
					TestApp.b.get(i).friends = p.friends;
				}
			}
		}
    }
}
