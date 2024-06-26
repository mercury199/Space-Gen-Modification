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

import static com.zarkonnen.spacegen.Main.add;
import static com.zarkonnen.spacegen.Main.animate;
import static com.zarkonnen.spacegen.Main.confirm;
import static com.zarkonnen.spacegen.Stage.add;
import static com.zarkonnen.spacegen.Stage.change;
import static com.zarkonnen.spacegen.Stage.delay;
import static com.zarkonnen.spacegen.Stage.tracking;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

public class SpaceGen {
	static Random r;
	ArrayList<String> log = new ArrayList<String>();
	ArrayList<Planet> planets = new ArrayList<Planet>();
	ArrayList<Civ> civs = new ArrayList<Civ>();
	ArrayList<String> historicalCivNames = new ArrayList<String>();
	ArrayList<Agent> agents = new ArrayList<Agent>();
	ArrayList<String> historicalSentientNames = new ArrayList<String>();
	ArrayList<String> turnLog = new ArrayList<String>();
	boolean clearTurnLogOnNewEntry = false;
	boolean hadCivs = false;
	boolean yearAnnounced = false;
	int year = 0;
	int age = 1;

	public static void main(String[] args) {
		SpaceGen sg = new SpaceGen(args.length > 1 ? Long.parseLong(args[1]) : System.currentTimeMillis());
		int bound = args.length > 0 ? Integer.parseInt(args[0]) : 650;
		while (!sg.interesting(bound)) {
			sg.tick();
		}
		sg.l("");
		sg.l("");
		sg.l(sg.describe());

		for (String le : sg.log) {
			System.out.println(le);
		}
	}

	boolean interesting(int bound) {
		// each civ gives a point
		// each hoard, art and wreck gives a point
		int pts = 0;
		pts += civs.size() * 100;
		pts += year / 6;
		for (Planet p : planets) {
			pts += p.lifeforms.size() * 5;
			pts += p.specials.size() * 15;
			pts += p.population();
			for (Stratum s : p.strata) {
				if (s instanceof LostArtefact) {
					LostArtefact la = (LostArtefact) s;
					if (la.artefact.type == ArtefactType.WRECK) {
						pts += 20;
					}
					if (la.artefact.type == ArtefactType.PIRATE_HOARD) {
						pts += 15;
					}
					if (la.artefact.type == ArtefactType.TIME_ICE) {
						pts += 10;
					}
					pts += 5;
				}
			}
			pts += p.plagues.size() * 15;
		}

		pts += agents.size() * 25;

		return year > bound / 4 && pts > bound;
	}

	public SpaceGen(long seed) {
		r = new Random(seed);
	}

	void init() {
		animate(delay(10));
		l("IN THE BEGINNING, ALL WAS DARK.");
		l("THEN, PLANETS BEGAN TO FORM:");
		int np = 11 + d(7, 4); // numbers planets here
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < np; i++) {
			Planet p = new Planet(r, this);
			sb.append(p.name).append(i == np - 1 ? "" : ", ");
			planets.add(p);
			add(add(p.sprite));
		}
		animate();
		l(sb.toString());
		confirm();
	}

	public boolean checkCivDoom(Civ c) {
		if (c.fullColonies().isEmpty()) {
			l("The $name collapses.", c);
			for (Planet out : new ArrayList<Planet>(c.getColonies())) {
				out.deCiv(year, null, "during the collapse of the " + c.name);
			}
			confirm();
			return true;
		}
		if (c.getColonies().size() == 1 && c.getColonies().get(0).population() == 1) {
			Planet remnant = c.getColonies().get(0);
			l("The $cname collapses, leaving only a few survivors on $pname.", c, remnant);
			remnant.setOwner(null);
			confirm();
			return true;
		}
		return false;
	}

	public void tick() {
		turnLog.clear();
		year++;
		yearAnnounced = false;
		if (!hadCivs && !civs.isEmpty()) {
			l("WE ENTER THE " + Names.nth(age).toUpperCase() + " AGE OF CIVILISATION");
			System.out.println("WE ENTER THE " + Names.nth(age).toUpperCase() + " AGE OF CIVILISATION");
			confirm();
		}
		if (hadCivs && civs.isEmpty()) {
			age++;
			l("WE ENTER THE " + Names.nth(age).toUpperCase() + " AGE OF DARKNESS");
			System.out.println("WE ENTER THE " + Names.nth(age).toUpperCase() + " AGE OF Darkness");
			confirm();
		}
		hadCivs = !civs.isEmpty();

		planets: for (Planet planet : planets) {
			if (p(2500)) {
				String col = pick(Names.COLORS);
				String mType = pick(AgentType.MONSTER_TYPES);
				String mName = "giant spaceborne " + col.toLowerCase() + " " + mType;
				l("A " + mName + " appears from the depths of space and menaces the skies of $name.", planet);
				Agent m = new Agent(AgentType.SPACE_MONSTER, year, mName, this);
				m.mType = mType;
				m.color = col;
				m.setLocation(planet);
				agents.add(m);
				confirm();
			}

			if ((planet.population() > 12 || (planet.population() > 7 && p(10)) && planet.getPollution() < 4)) {
				planet.setPollution(planet.getPollution() + 1);
			}
			for (Population pop : new ArrayList<Population>(planet.inhabitants)) {
				if (planet.getOwner() == null && p(100) && pop.type.base != SentientType.Base.ROBOTS
						&& pop.type.base != SentientType.Base.PARASITES) {
					SentientType nst = pop.type.mutate(this, null);
					l("The $sname on $pname mutate into " + nst.getName() + ".", pop.type, planet);
					pop.type = nst;
					pop.update();
					confirm();
				}
				int modifier = 0;
				if (planet.getOwner() != null)
					modifier = planet.getOwner().getConstitution();
				int roll = d(6) + modifier;
				if (roll < planet.getPollution()) {
					System.out.println("Pollution amount " + planet.getPollution());
					l("Pollution kills a billion $sname on $pname.", pop.type, planet);
					planet.setPollution(planet.getPollution() - 1);
					if (pop.getSize() == 1) {
						pop.eliminate();
						planet.dePop(pop, year, null, "from the effects of pollution", null);
						l("$sname have died out on $pname!", pop.type, planet);
						confirm();
						continue planets;
					} else {
						pop.setSize(pop.getSize() - 1);
					}
				} else {
					if (roll == 6 || (pop.type.base == SentientType.Base.ANTOIDS && roll > 3)
							|| (planet.getOwner() != null && roll == 5)
							|| (planet.has(SentientType.Base.ANTOIDS.specialStructure) && roll > 2)) {
						pop.setSize(pop.getSize() + 1);
						l("The population of $sname on $pname has grown by a billion.", pop.type, planet);
					}
				}
				if (pop.type.base == SentientType.Base.KOBOLDOIDS && p(10)
						&& planet.has(SentientType.Base.KOBOLDOIDS.specialStructure)) {
					pop.setSize(pop.getSize() + 1);
					l("The skull pit on $pname excites the local $sname into a sexual frenzy.", pop.type, planet);
				}
				if (pop.getSize() > 3 && pop.type.base == SentientType.Base.KOBOLDOIDS && p(20)) {
					l("The $sname on $pname devour one billion of their own kind in a mad frenzy of cannibalism!",
							pop.type, planet);
					if (planet.getOwner() != null && !planet.has(StructureType.Standard.SKULL_PILE)
							&& planet.structures.size() < 5) {
						planet.addStructure(new Structure(StructureType.Standard.SKULL_PILE, planet.getOwner(), year));
						l("The $sname erect a pile of skulls on $pname!", pop.type, planet);
						confirm();
					}
				}

				for (Plague plague : new ArrayList<Plague>(planet.plagues)) {
					if (plague.affects.contains(pop.type)) {
						int con_mod=0;
						if(planet.getOwner()!=null){
							con_mod = planet.getOwner().getConstitution();
						}
						if (d(12) + con_mod < plague.lethality) {
							if (pop.getSize() <= 1) {
								planet.dePop(pop, year, null, "from the " + plague.name, new Plague(plague));
								l("The $sname on $pname have been wiped out by the " + plague.name + "!", pop.type,
										planet);
							} else {
								pop.setSize(pop.getSize() - 1);
							}
						}
					} else {
						if (d(12) < plague.mutationRate && pop.type.base != SentientType.Base.ROBOTS) {
							plague.affects.add(pop.type);
							l("The " + plague.name + " mutates to affect $name", pop.type);
						}
					}
				}
			}

			for (Plague plague : new ArrayList<Plague>(planet.plagues)) {
				int int_mod = 0;
				if (planet.getOwner() != null){			
					int_mod = planet.getOwner().getIntelligence();
				}	
				if (d(12) + int_mod < plague.curability) {
					planet.removePlague(plague);
					l(plague.name + " has been eradicated on $name.", planet);
				} else {
					if (d(12) < plague.transmissivity) {
						Planet target = pick(planets);
						boolean canJump = false;
						for (Population pop : target.inhabitants) {
							if (plague.affects.contains(pop.type)) {
								canJump = true;
							}
						}
						if (canJump) {
							boolean match = false;
							for (Plague p2 : target.plagues) {
								if (p2.name.equals(plague.name)) {
									for (SentientType st : plague.affects) {
										if (!p2.affects.contains(st)) {
											p2.affects.add(st);
										}
										match = true;
									}
								}
							}
							if (!match) {
								target.addPlague(new Plague(plague));
							}
						}
					}
				}
			}
			
		}

		// TICK CIVS
		for (Civ c : new ArrayList<Civ>(civs)) {
			if (checkCivDoom(c)) {
				civs.remove(c);
				continue;
			}
			int newRes = 0;
			int newSci = 1;
			if(c.getIntelligence()>0){
				int ccval = d(6)+c.getIntelligence();
				if(ccval>=5){
					newSci++;
					if(ccval>=10){
						newSci+=2;
					}
				}
			}
			for (Planet col : new ArrayList<Planet>(c.getColonies())) {
				if (c.has(ArtefactType.Device.UNIVERSAL_ANTIDOTE)) {
					for (Plague p : col.plagues) {
						l("The " + p.name + " on $name is cured by the universal antidote.", col);
					}
					col.clearPlagues();
				}
				if (col.population() > 7 || (col.population() > 4 && p(3))) {
					col.evoPoints = 0;
					// Test the civ against this.
					int hazardPrep = (col.getOwner().getResources() + col.getOwner().getTechLevel()
							+ col.getOwner().getScience());
					if (col.population() * 4 >= d(hazardPrep, 6))
						col.setPollution(col.getPollution() + 1);
					l("Overcrowding on $name leads to increased pollution.", col);
				}
				if (p(6) && col.population() > 4 && col != c.leastPopulousFullColony()
						&& c.leastPopulousFullColony().population() < col.population() - 1) {
					for (Population pop : col.inhabitants) {
						if (pop.getSize() > 1) {
							pop.send(c.leastPopulousFullColony());
							break;
						}
					}
				}
				if (c.has(ArtefactType.Device.MIND_READER) && p(4)) {
					for (Population pop : col.inhabitants) {
						pop.setSize(pop.getSize() + 1);
					}
				}
				if (col.population() == 0 && !col.isOutpost()) {
					col.deCiv(year, null, "");
				} else {
					if (col.population() > 0) {
						newRes++;
						if (col.lifeforms.contains(SpecialLifeform.VAST_HERDS)) {
							newRes++;
						}
					}
					if (col.specials.contains(PlanetSpecial.GEM_WORLD)) {
						newRes++;
					}
					if (col.has(StructureType.Standard.MINING_BASE)) {
						newRes += 1;
					}
					if (col.has(StructureType.Standard.SCIENCE_LAB)) {
						newSci += 2;
					}
					if (col.has(SentientType.Base.PARASITES.specialStructure)) {
						newSci += 2;
					}
					if (col.has(SentientType.Base.TROLLOIDS.specialStructure)) {
						newSci += 2;
					}
				}
			}

			if (checkCivDoom(c)) {
				civs.remove(c);
				continue;
			}

			if (c.has(ArtefactType.Device.MASTER_COMPUTER)) {
				newRes += 2;
				newSci += 3;
			}

			c.setResources(c.getResources() + newRes);

			SentientType lead = pick(c.fullMembers);
			pick(lead.base.behaviour).invoke(c, this);
			if (checkCivDoom(c)) {
				civs.remove(c);
				continue;
			}
			pick(c.getGovt().behaviour).invoke(c, this);
			if (checkCivDoom(c)) {
				civs.remove(c);
				continue;
			}

			c.setScience(c.getScience() + newSci);

			if (c.getScience() > c.nextBreakthrough) {
				c.setScience(c.getScience() - c.nextBreakthrough);
				if (Science.advance(c, this)) {
					continue;
				}
				c.nextBreakthrough = Math.min(500, c.nextBreakthrough * 3 / 2);
			}

			int cAge = year - c.birthYear;
			if (cAge > 5) {
				c.decrepitude++;
			}
			if (cAge > 15) {
				c.decrepitude++;
			}
			if (cAge > 25) {
				c.decrepitude++;
			}
			if (cAge > 40) {
				c.decrepitude++;
			}
			if (cAge > 60) {
				c.decrepitude++;
			}
			if (cAge > 100) {
				c.decrepitude++;
			}
			if (cAge > 200) {
				c.decrepitude++;
			}
			if (cAge > 300) {
				c.decrepitude++;
			}
			if (p(3)) {
				int evtTypeRoll = d(12);
				boolean good;
				boolean bad;
				if (c.decrepitude < 5 && cAge < 100) {
					good = evtTypeRoll <= 5;
					bad = false;
				} else if (c.decrepitude < 17 && cAge > 75) {
					good = evtTypeRoll >= 6;
					bad = evtTypeRoll < 3;
				} else if (c.decrepitude < 17) {
					good = evtTypeRoll >= 6;
					bad = evtTypeRoll == 1;
				} else if (c.decrepitude < 25) {
					good = evtTypeRoll >= 8;
					bad = evtTypeRoll < 4;
				} else if (c.decrepitude < 40) {
					good = evtTypeRoll >= 9;
					bad = evtTypeRoll < 6;
				} else {
					good = evtTypeRoll > 10;
					bad = evtTypeRoll < 8;
				}

				if (good) {
					pick(GoodCivEvent.values()).invoke(c, this);
				}
				if (checkCivDoom(c)) {
					writeCivToLog(c);
					civs.remove(c);
					continue;
				}
				if (bad) {
					pick(BadCivEvent.values()).invoke(c, this);
				}

				if (checkCivDoom(c)) {
					writeCivToLog(c);
					civs.remove(c);
					continue;
				}
			}

			War.doWar(c, this);
		}

		// TICK AGENTS
		for (Agent a : new ArrayList<Agent>(agents)) {
			if (!agents.contains(a)) {
				continue;
			}
			a.type.behave(a, this);
		}
		for (Agent a : new ArrayList<Agent>(agents)) {
			if (!agents.contains(a)) {
				continue;
			}
			if (a.type == AgentType.ADVENTURER) {
				a.type.behave(a, this);
			}
		}

		// TICK PLANETS
		for (Planet p : planets) {
			if (p.habitable && p(500)) {
				Cataclysm c = pick(Cataclysm.values());
				Civ civ = p.getOwner();
				l(c.desc, p);
				p.deLive(year, c, null);

				if (civ != null) {
					if (checkCivDoom(civ)) {
						civs.remove(civ);
					}
				}
				confirm();
				continue;
			}

			if (p(200) && p.getPollution() > 1 && !p.specials.contains(PlanetSpecial.POISON_WORLD)) {
				l("Pollution on $name abates.", p);
				p.setPollution(p.getPollution() - 1);
			}

			if (p(300 + 5000 * p.specials.size())) {
				PlanetSpecial ps = pick(PlanetSpecial.values());
				if (!p.specials.contains(ps)) {
					p.specials.add(ps);
					ps.apply(p);
					l(ps.announcement, p);
					if (p.specials.size() == 1) {
						animate(tracking(p.sprite, change(p.sprite, Imager.get(p))));
						confirm();
					}
				}
			}
			//int evo_orig = p.evoPoints;
			p.evoPoints += (d(6)+1) * d(6) * d(6) * d(6) * d(6) * 3 * (6 - p.getPollution());
			//System.out.println("Evo Points Gained: " + (p.evoPoints - evo_orig));
			if (p.evoPoints > p.evoNeeded && p(12) && p.getPollution() < 2) {
				p.evoPoints = 0;
				if (!p.habitable) {
					p.habitable = true;
					animate(tracking(p.sprite, change(p.sprite, Imager.get(p))));
					l("Life arises on $name", p);
					confirm();
				} else {
					if (!p.inhabitants.isEmpty() && coin()) {
						if (p.getOwner() == null) {
							// Do the civ thing.
							Government g = pick(Government.values());
							Population starter = pick(p.inhabitants);
							starter.setSize(starter.getSize() + 1);
							Civ c = new Civ(year, starter.type, p, g, d(3), this);
							l("The $sname on $pname achieve spaceflight and organise as a " + g.typeName + ", the "
									+ c.name + ".", starter.type, p);
							historicalCivNames.add(c.name);
							civs.add(c);
							for (Population pop : p.inhabitants) {
								pop.addUpdateImgs();
							}
							turnLog.add("\nNew Civ Created\n");
							writeCivToLog(c);
							animate();
							confirm();
						}
					} else {
						if (p(3) || p.lifeforms.size() >= 3) {
							// Sentient!
							SentientType st = SentientType.invent(this, null, p, null);
							new Population(st, 2 + d(1), p);
							l("Sentient $sname arise on $pname.", st, p);
							confirm();
						} else {
							// Some special creature.
							SpecialLifeform slf = pick(SpecialLifeform.values());
							if (!p.lifeforms.contains(slf)) {
								p.addLifeform(slf);
								l("$lname evolve on $pname.", slf, p);
								confirm();
							}
						}
					}
				}
			}
		}

		// Erosion
		for (Planet p : planets) {
			for (Stratum s : new ArrayList<Stratum>(p.strata)) {
				int sAge = year - s.time() + 1;
				if (s instanceof Fossil) {
					if (sAge < 0) {
						p.strata.remove(s);
					} else if (p(12000 / sAge + 800)) {
						p.strata.remove(s);
					}
				}
				if (s instanceof LostArtefact) {
					if (((LostArtefact) s).artefact.type == ArtefactType.Device.STASIS_CAPSULE) {
						continue;
					}
					if (sAge < 0) {
						p.strata.remove(s);
					} else if (p(10000 / sAge + 500)) {
						p.strata.remove(s);
					}
				}
				if (s instanceof Remnant) {
					if (sAge < 0) {
						p.strata.remove(s);
					}
					if (p(4000 / sAge + 400)) {
						p.strata.remove(s);
					}
				}
				if (s instanceof Ruin) {
					Ruin ruin = (Ruin) s;
					if (ruin.structure.type == StructureType.Standard.MILITARY_BASE
							|| ruin.structure.type == StructureType.Standard.MINING_BASE
							|| ruin.structure.type == StructureType.Standard.SCIENCE_LAB) {
						if (sAge < 0) {
							p.strata.remove(s);
						} else if (p(1000 / sAge + 150)) {
							p.strata.remove(s);
						}
					} else {
						if (sAge < 0) {
							p.strata.remove(s);
						} else if (p(3000 / sAge + 300)) {
							p.strata.remove(s);
						}
					}
				}
			}
		}

		/*
		 * for (Planet p : planets) { if (p.owner != null) { if
		 * (!p.owner.colonies.contains(p)) { System.out.println("OMG BBQ WTF A"); } if
		 * (!civs.contains(p.owner)) { System.out.println("OMG BBQ WTF B"); } } } for
		 * (Civ c : civs) { for (Planet col : c.colonies) { if (col.owner != c) {
		 * System.out.println("OMG BBQ WTF C"); } } }
		 */
	}

	public void writeCivToLog(Civ aCiv) {
		if (aCiv != null) {
			String data = "";
			data += "\nCiv Name: " + aCiv.name;
			data += " \nCiv Birth Year: " + aCiv.birthYear;
			data += " \nCiv Stats Str|Dex|Con|Int|Cha\n" + "          " + aCiv.getStrength() + " | "
					+ aCiv.getDexterity() + " | " + aCiv.getConstitution() + " | " + aCiv.getIntelligence() + " | "
					+ aCiv.getCharisma() + " | ";
			data += "\nResources " + aCiv.getResources() + " Science " + aCiv.getScience() + " Military "
					+ aCiv.getMilitary() + " WeapLevel " + aCiv.getWeapLevel() + " TechLevel " + aCiv.getTechLevel()
					+ " Decrepitude " + aCiv.decrepitude;
			l(data);
		}
	}

	public String describe() {
		StringBuilder sb = new StringBuilder();

		// Critters
		HashSet<SentientType> sts = new HashSet<SentientType>();
		for (Planet p : planets) {
			for (Population pop : p.inhabitants) {
				sts.add(pop.type);
			}
		}

		if (sts.size() > 0) {
			sb.append("SENTIENT SPECIES:\n");
		}

		for (SentientType st : sts) {
			sb.append(st.getName()).append(": ").append(st.getDesc());
			sb.append("\n");
		}

		if (civs.size() > 0) {
			sb.append("\nCIVILISATIONS:\n");
		}
		for (Civ c : civs) {
			sb.append(c.fullDesc(this));
			sb.append("\n");
		}

		sb.append("PLANETS:\n");
		for (Planet p : planets) {
			sb.append(p.fullDesc(this));
			sb.append("\n");
		}

		return sb.toString();
	}

	final <T> T pick(ArrayList<T> ts) {
		return ts.get(r.nextInt(ts.size()));
	}

	final <T> T pick(T[] ts) {
		return ts[r.nextInt(ts.length)];
	}

	final void l(String s, Planet p) {
		l(s.replace("$name", p.name));
	}

	final void l(String s, SentientType st) {
		l(s.replace("$name", st.getName()));
	}

	final void l(String s, Civ st) {
		l(s.replace("$name", st.name));
	}

	final void l(String s, Civ st, Planet p) {
		l(s.replace("$cname", st.name).replace("$pname", p.name));
	}

	final void l(String s, SentientType st, Planet p) {
		l(s.replace("$sname", st.getName()).replace("$pname", p.name));
	}

	final void l(String s, SpecialLifeform slf, Planet p) {
		l(s.replace("$lname", slf.name).replace("$pname", p.name));
	}

	final void l(String s) {
		if (clearTurnLogOnNewEntry) {
			turnLog.clear();
			clearTurnLogOnNewEntry = false;
		}
		if (!yearAnnounced) {
			yearAnnounced = true;
			l(year + ":");
		}
		// System.out.println(s);
		log.add(s);
		turnLog.add(s);
	}

	final boolean coin() {
		return r.nextBoolean();
	}

	final boolean p(int n) {
		return d(n) == 0;
	}

	final boolean atLeast(int requirement, int n) {
		return requirement >= d(n);
	}

	final boolean lessThan(int tooMuch, int n) {
		return tooMuch <= d(n);
	}

	public final static int d(int n) {
		if (n < 1)
			return 1;
		else
			return r.nextInt(n);
	}

//TODO made these static, fix if it's an issue. 
	public final static int d(int rolls, int n) {
		int sum = 0;
		if (rolls < 0)
			rolls = 0;
		for (int roll = 0; roll < rolls; roll++) {
			sum += d(n);
		}
		return sum;
	}

	void tickUntilSomethingHappens() {
		turnLog.clear();
		while (turnLog.isEmpty()) {
			tick();
		}
	}
}
