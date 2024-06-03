/**
Copyright 2012 David Stark

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package com.zarkonnen.spacegen;

import static com.zarkonnen.spacegen.Main.animate;
import static com.zarkonnen.spacegen.Main.confirm;
import static com.zarkonnen.spacegen.Stage.change;
import static com.zarkonnen.spacegen.Stage.tracking;
import java.util.ArrayList;

public class Science {

 	static int multi_sci_val = 0;

	static boolean advance(Civ actor, SpaceGen sg) {
		ArrayList<Planet> cands;
		int value = sg.d(9);
		System.out.println(" Value in science roll : " + value + " Science amount: " + (actor.getScience() + actor.nextBreakthrough));
		int mult_sci_roll = sg.d(100)+1+actor.getIntelligence()+actor.getScience()/2;
		//while(mult_sci_roll > 0){} // emable multi-sci options later
		switch (value) {
		case 0:
				actor.setTechLevel(actor.getTechLevel() + 1);
	//TODO make it so they can make their stats better
				int tech_roll = sg.d(4)+1+actor.getTechLevel();
				System.out.println("In science, number 0 chosen Tech Roll : " + tech_roll + " Tech Score : " + actor.getTechLevel() + " .");
				if (tech_roll > 2) {

					int dice = actor.getTechLevel() + actor.getIntelligence();
					int stat = sg.d(5);
					int statval = 0;
					if (stat == 1)
						statval = 4 + actor.getStrength();
					else if (stat == 2)
						statval = 4 + actor.getDexterity();
					else if (stat == 3)
						statval = 4 + actor.getConstitution();
					else if (stat == 4)
						statval = 4 + actor.getCharisma();
					else
						statval = 4 + actor.getIntelligence();

					if(statval < 0){
						statval = 0;
					}
					if(dice<=0){
						dice = 1;
					}
					int checkDif = statval * (6+(statval-4));
					int roll = sg.d(dice, actor.getTechLevel()+1) + dice + actor.getScience();
					sg.l("The $name attempting to upgrade their stats of value :" + stat + " current score is: "
							+ (statval - 4) + "\nThe difficulty is: " + checkDif + " They rolled: " + roll + "on: " + dice +"d"+(actor.getTechLevel()+1)+"+"+actor.getScience()+"\nTechLevel: " + actor.getTechLevel() + " Science: " + actor.getScience(), actor);
					System.out.println("The $name attempting to upgrade their stats of value :" + stat + " current score is: "+ (statval - 4) + "\nThe difficulty is: " + checkDif + " They rolled: " + roll + "on: " + dice +"d"+(actor.getTechLevel()+1)+"+"+actor.getScience()+"\nTechLevel: " + actor.getTechLevel() + " Science: " + actor.getScience());
					if (roll >= checkDif && roll < checkDif*2) {
						if (stat == 1)
							actor.Strength++;
						else if (stat == 2)
							actor.Dexterity++;
						else if (stat == 3)
							actor.Constitution++;
						else if (stat == 4)
							actor.Charisma++;
						else
							actor.Intelligence++;
					} else if(roll >= checkDif*2) {
						String super_upgrade = "\n Critical Success on Stat Updage. All stats will be tested.";
						statval = 4 + actor.getStrength();
						checkDif = (statval * (6+(statval-4)))*(2/3);
						if(roll>=checkDif){
							actor.Strength++;
							System.out.println("Super roll upgraded strength!");
							super_upgrade += "Strength upgraded from: " + (actor.getStrength()-1);
						}

						statval = 4 + actor.getDexterity();
						checkDif = (statval * (6+(statval-4)))*(2/3);
						if(roll>=checkDif){
							actor.Dexterity++;
							System.out.println("Super roll upgraded Dexterity!");
							super_upgrade += "Dexterity upgraded from: " + (actor.getDexterity()-1);
						}

						statval = 4 + actor.getConstitution();
						checkDif = (statval * (6+(statval-4)))*(2/3);
						if(roll>=checkDif){
							actor.Constitution++;
							System.out.println("Super roll upgraded Constitution!");
							super_upgrade += "Constitution upgraded from: " + (actor.getConstitution()-1);
						}

						statval = 4 + actor.getIntelligence();
						checkDif = (statval * (6+(statval-4)))*(2/3);
						if(roll>=checkDif){
							actor.Intelligence++;
							System.out.println("Super roll upgraded Intelligence!");
							super_upgrade += "Intelligence upgraded from: " + (actor.getIntelligence()-1);
						}

						statval = 4 + actor.getCharisma();
						checkDif = (statval * (6+(statval-4)))*(2/3);
						if(roll>=checkDif){
							actor.Charisma++;
							System.out.println("Super roll upgraded Charisma!");
							super_upgrade += "Charisma upgraded from: " + (actor.getCharisma()-1);
						}


						actor.Strength++;
						actor.Dexterity++;
						actor.Constitution++;
						actor.Charisma++;
						actor.Intelligence++;
						sg.l("The $name achieved a " + super_upgrade, actor);
						System.out.println("Super Upgrade! in science stats " + super_upgrade);
					}

				}

				if (actor.getTechLevel() >= 14
						&& (32 <= SpaceGen.d(2, 10) + actor.getIntelligence() + actor.getTechLevel())) {
					sg.l("The highly advanced technology of the" +actor.name+" allows them to transcend the bounds of this universe. They vanish instantly.");
					for (Planet col : new ArrayList<Planet>(actor.getColonies())) {
						col.transcend(sg.year);
					}
					sg.civs.remove(actor);
					confirm();
					return true;
				}
				break;
			case 1:
				// Develop new weapons systems.
				sg.l("The $name develop powerful new weapons.", actor);
				actor.setWeapLevel(actor.getWeapLevel() + 1);
				confirm();
				break;
			case 2:
				Planet srcP = actor.largestColony();
				if (srcP.population() > 1) {
					for (Planet p : actor.reachables(sg)) {
						if (!p.habitable && p.getOwner() == null) {
							p.habitable = true;
							// p.inhabitants.add(new Population(actor.fullMembers.get(0), 1));

							Population srcPop = null;
							for (Population pop : srcP.inhabitants) {
								if (actor.fullMembers.contains(pop.type) && pop.getSize() > 1) {
									srcPop = pop;
								}
							}
							if (srcPop == null) {
								srcPop = sg.pick(srcP.inhabitants);
							}
							srcPop.send(p);

							p.setOwner(actor);
							animate(tracking(p.sprite, change(p.sprite, Imager.get(p))));
							sg.l("The $cname terraform and colonise $pname.", actor, p);
							confirm();
							return false;
						}
					}
				}
				// INTENTIONAL FALLTHROUGH
			case 3:
				for (Planet p : actor.reachables(sg)) {
					if (p.habitable && p.getOwner() == null && p.inhabitants.isEmpty()) {
						SentientType st = SentientType.invent(sg, actor, p, null);
						p.setOwner(actor);
						new Population(st, 3, p);
						sg.l("The $cname uplift the local " + st.getName()
								+ " on $pname and incorporate the planet into their civilisation.", actor, p);
						confirm();
						return false;
					}
				}
			case 4:
				// ROBOTS!
				cands = new ArrayList<Planet>();
				lp: for (Planet p : actor.fullColonies()) {
					for (Population pop : p.inhabitants) {
						if (pop.type.base == SentientType.Base.ROBOTS) {
							continue lp;
						}
					}
					cands.add(p);
				}
				if (cands.isEmpty()) {
					return false;
				}
				Planet rp = sg.pick(cands);
				SentientType rob = SentientType.genRobots(sg, actor, rp, null);
				sg.l("The $cname create " + rob.getName() + " as servants on $pname.", actor, rp);
				new Population(rob, 4, rp);
				confirm();
				break;
			case 5:
				Planet target = actor.largestColony();
				if (target == null) {
					return false;
				}
				Agent probe = new Agent(AgentType.SPACE_PROBE, sg.year,
						sg.pick(new String[] { "Soj'r", "Monad", "Lun'hod", "Mar'er", "P'neer", "Dyad", "Triad" }), sg);
				probe.target = target;
				probe.timer = 8 + sg.d(25);
				probe.originator = actor;
				sg.l("The $name launch a space probe called " + probe.name + " to explore the galaxy.", actor);
				sg.agents.add(probe);
				confirm();
				break;
			case 6:
					actor.setTechLevel(actor.getTechLevel() + 1);
				break;
			case 7:
				if (actor.getIntelligence() > 1) {
					actor.setTechLevel(actor.getTechLevel() + 1);
					if (actor.getTechLevel() >= 14
							&& (32 <= SpaceGen.d(2, 10) + actor.getIntelligence() + actor.getTechLevel())) {
						sg.l("The highly advanced technology of the " + actor.name + " allows them to transcend the bounds of this universe. They vanish instantly.");
						for (Planet col : new ArrayList<Planet>(actor.getColonies())) {
							col.transcend(sg.year);
						}
						sg.civs.remove(actor);
						confirm();
						return true;
					}
				}
				break;

			case 8:
				cands = new ArrayList<Planet>();
				for (Planet p : actor.getColonies()) {
					if (p.has(StructureType.Standard.SCIENCE_LAB)) {
						cands.add(p);
					}
				}
				cands.add(actor.largestColony());
				Planet p = sg.pick(cands);
				ArtefactType.Device type = sg.pick(ArtefactType.Device.values());
				Artefact a = new Artefact(sg.year, actor, type, type.create(actor, sg));
				p.addArtefact(a);
				sg.l("The $name develop a " + a.type.getName() + ".", actor);
				confirm();
			}
			
		return false;
	}

	

}
