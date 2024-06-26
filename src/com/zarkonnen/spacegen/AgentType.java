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

import java.util.ArrayList;

import static com.zarkonnen.spacegen.Stage.*;
import static com.zarkonnen.spacegen.Main.*;

public enum AgentType {
	PIRATE() {
		@Override
		public String describe(Agent a, SpaceGen sg) {
			String d = "In orbit: The pirate " + a.name + ", a " + a.st.name;
			if (a.fleet < 2) {
				d += ".";
			} else {
				d += ", commanding a fleet of " + a.fleet + " ships.";
			}
			return d;
		}
		
		@Override
		public void behave(Agent a, SpaceGen sg) {
			// move
			a.setLocation(sg.pick(sg.planets));
			int age = sg.year - a.birth;
			if (age > 8 + sg.d(6)) {
				sg.l("The pirate " + a.name + " dies and is buried on " + a.getLocation().name + ".");
				Artefact art = new Artefact(sg.year, (Civ) null, ArtefactType.PIRATE_TOMB, "Tomb of the Pirate " + a.name);
				art.specialValue = a.resources + a.fleet;
				a.getLocation().strata.add(new LostArtefact("buried", sg.year, art));
				a.setLocation(null);
				sg.agents.remove(a);
				confirm();
				return;
			}
			if (a.getLocation().getOwner() != null) {
				int tribute = sg.d(8) + 1;
				if (a.getLocation().getOwner().getResources() >= tribute && !sg.p(4)) {
					a.getLocation().getOwner().setResources(a.getLocation().getOwner().getResources() - tribute);
					a.resources += tribute;
					sg.l("The pirate " + a.name + " receives tribute from " + a.getLocation().name + " of the " + a.getLocation().getOwner().name + ".");
				} else {
					int attack = a.fleet * 4;
					int defence = a.getLocation().population() + (a.getLocation().has(StructureType.Standard.MILITARY_BASE) ? 5 * (a.getLocation().getOwner().getTechLevel() + 2 * a.getLocation().getOwner().getWeapLevel()) : 0);
					if (a.getLocation().has(SentientType.Base.URSOIDS.specialStructure)) {
						defence += 4;
					}
					int attackRoll = sg.d(attack, 6);
					int defenceRoll = sg.d(defence, 6);
					Planet target = a.getLocation();
					if (attackRoll > defenceRoll) {
						if (target.has(SentientType.Base.DEEP_DWELLERS.specialStructure)) {
							for (Structure st : new ArrayList<Structure>(target.structures)) {
								if (sg.p(3)) {
									target.strata.add(new Ruin(st, sg.year, null, "through orbital bombardment by the pirate " + a.name));
									target.removeStructure(st);
								}
							}
							sg.l("The pirate " + a.name + " subjects " + target.name + " to orbital bombardment. Its inhabitants hide in the dome deep in the planet's crust and escape harm.");
							confirm();
							return;
						}
						int deaths = 0;
						for (Population pop : new ArrayList<Population>(target.inhabitants)) {
							int pd = sg.d(pop.getSize()) + 1;
							if (pd >= pop.getSize()) {
								target.dePop(pop, sg.year, null, "due to orbital bombardment by the pirate " + a.name, null);
							} else {
								pop.setSize(pop.getSize() - pd);
							}
							deaths += pd;
						}
						if (target.population() == 0) {
							target.deCiv(sg.year, null, "due to orbital bombardment by the pirate " + a.name);
							sg.l("The pirate " + a.name + " subjects " + target.name + " to orbital bombardment.");
						} else {
							for (Structure st : new ArrayList<Structure>(target.structures)) {
								if (sg.coin()) {
									target.strata.add(new Ruin(st, sg.year, null, "through orbital bombardment by the pirate " + a.name));
									target.removeStructure(st);
								}
							}
							sg.l("The pirate " + a.name + " subjects " + target.name + " to orbital bombardment, killing " + deaths + " billion.");
						}
					} else {
						sg.l("The $name defeats the pirate " + a.name + ".", a.getLocation().getOwner());
						a.getLocation().getOwner().setResources(a.getLocation().getOwner().getResources() + a.resources / 2);
						a.getLocation().getOwner().setMilitary(a.getLocation().getOwner().getMilitary() * 5 / 6);
						a.setLocation(null);
						sg.agents.remove(a);
					}
					confirm();
				}
			} else {
				// Buy more ships or leave pirate treasure.
				if (a.resources > 5) {
					if (sg.p(3)) {
						sg.l("The pirate " + a.name + " buries a hoard of treasure on " + a.getLocation().name + ".");
						Artefact art = new Artefact(sg.year, (Civ) null, ArtefactType.PIRATE_HOARD, "Hoard of the Pirate " + a.name);
						art.specialValue = a.resources - 2;
						a.resources = 2;
						a.getLocation().strata.add(new LostArtefact("buried", sg.year, art));
						confirm();
					} else {
						a.fleet++;
						a.resources -= 2;
					}
				}
			}
		}
	},
	ADVENTURER() {
		@Override
		public String describe(Agent a, SpaceGen sg) {
			String d = "In orbit: The adventurer " + a.name + ", a member of the " + a.st.name + ", serving the " + a.originator.name;
			if (a.fleet < 2) {
				d += ".";
			} else {
				d += ", commanding a fleet of " + a.fleet + " ships.";
			}
			return d;
		}
		
		boolean encounter(Agent a, SpaceGen sg, Agent ag) {
			switch (ag.type) {
				case ROGUE_AI:
					if (sg.coin()) {
						if (a.fleet <= 3) {
							sg.l(a.name + " is killed by the rogue AI " + ag.name + ".");
							sg.agents.remove(a);
							a.getLocation().strata.add(new LostArtefact("crashed", sg.year, new Artefact(sg.year, a.originator, ArtefactType.WRECK,
									"wreck of the flagship of " + a.name + ", destroyed by the rogue AI " + ag.name)));
							a.setLocation(null);
						} else {
							int loss = sg.d(a.fleet - 1) + 1;
							sg.l(a.name + " is attacked by the rogue AI " + ag.name + " and has to retreat, losing " + loss + " ships.");
							a.fleet -= loss;
							a.getLocation().strata.add(new LostArtefact("crashed", sg.year, new Artefact(sg.year, a.originator, ArtefactType.WRECK,
									"shattered wrecks of " + loss + " spaceships of the fleet of " + a.name + ", destroyed by the rogue AI " + ag.name)));
						}
					} else {
						sg.l(a.name + " manages to confuse the rogue AI " + ag.name + " with a clever logic puzzle, distracting it long enough to shut it down.");
						a.resources += 5;
						sg.agents.remove(ag);
						ag.setLocation(null);
					}
					confirm();
					return true;
				case SPACE_PROBE:
					if (sg.coin()) {
						sg.l(a.name + " attempts to reason with the space probe " + ag.name + " but triggers its self-destruct mechanism.");
						if (sg.coin()) {
							a.getLocation().deLive(sg.year, null, "due to the self-destruction of the insane space probe " + ag.name);
							sg.l("The resulting shockwave exterminates all life on " + a.getLocation().name + ".");
							sg.agents.remove(a);
							a.setLocation(null);
						}
						sg.agents.remove(ag);
						ag.setLocation(null);
					} else {
						sg.l(a.name + " successfully reasons with the insane space probe " + ag.name + ", which transfers its accumulated information into the fleet's data banks and then shuts down.");
						a.originator.setTechLevel(a.originator.getTechLevel() + 3);
						sg.agents.remove(ag);
						ag.setLocation(null);
					}
					confirm();
					return true;
				case SPACE_MONSTER:
					int attackRoll = sg.d(a.fleet, 6);
					int defenseRoll = sg.d(4, 6);
					if (attackRoll > defenseRoll) {
						sg.l(a.name + " defeats the " + ag.name + " in orbit around " + a.getLocation().name + ".");
						sg.agents.remove(ag);
						ag.setLocation(null);
						if (a.getLocation().getOwner() != null) {
							sg.l("The " + a.getLocation().getOwner().name + " rewards the adventurer handsomely.");
							a.resources += a.getLocation().getOwner().getResources() / 3;
							a.getLocation().getOwner().setResources(a.getLocation().getOwner().getResources() * 2 / 3);
						}
					} else {
						int loss = sg.d(2) + 2;
						if (a.fleet - loss <= 0) {
							sg.l("The " + ag.name + " in orbit around " + a.getLocation().name + " attacks and kills " + a.name + ".");
							sg.agents.remove(a);
							a.getLocation().strata.add(new LostArtefact("crashed", sg.year, new Artefact(sg.year, a.originator, ArtefactType.WRECK,
									"wreck of the flagship of " + a.name + ", destroyed by a " + ag.name)));
							a.setLocation(null);
						} else {
							a.fleet -= loss;
							sg.l("The " + ag.name + " attacks the fleet of " + a.name + " near " + a.getLocation().name + " destroying " + loss + " ships.");
							a.getLocation().strata.add(new LostArtefact("crashed", sg.year, new Artefact(sg.year, a.originator, ArtefactType.WRECK,
									"shattered wrecks of " + loss + " spaceships of the fleet of " + a.name + ", destroyed by a " + ag.name)));
						}
					}
					confirm();
					return true;
			}
			return false;
		}
		
		@Override
		public void behave(Agent a, SpaceGen sg) {
			a.setLocation(sg.pick(sg.planets));
			int age = sg.year - a.birth;
			if (!sg.civs.contains(a.originator) || age > 8 + sg.d(6)) {
				sg.l("The space adventurer " + a.name + " dies and is buried on " + a.getLocation().name + ".");
				Artefact art = new Artefact(sg.year, (Civ) null, ArtefactType.ADVENTURER_TOMB, "Tomb of " + a.name);
				a.getLocation().strata.add(new LostArtefact("buried", sg.year, art));
				art.specialValue = a.resources / 3 + a.fleet / 5 + 1;
				sg.agents.remove(a);
				a.setLocation(null);
				confirm();
				return;
			}
			
			for (Agent ag : sg.agents) {
				if (ag != a && ag.getLocation() == a.getLocation()) {
					if (encounter(a, sg, ag)) { return; }
				}
			}
			
			if (sg.p(3) && a.getLocation().getOwner() != null && a.getLocation().getOwner() != a.originator && a.originator.relation(a.getLocation().getOwner()) == Diplomacy.Outcome.WAR) {
				// Show some initiative!
				String act = sg.pick(new String[] {
					" raids the treasury on ",
					" intercepts a convoy near ",
					" steals jewels from ",
					" steals a spaceship from the navy of ",
					" extorts money from "
				});
				sg.l(a.name + act + a.getLocation().name + ", a planet of the enemy " + a.getLocation().getOwner().name + ".");
				a.resources += 2;
				a.getLocation().getOwner().setResources(a.getLocation().getOwner().getResources() * 5 / 6);
				confirm();
				return;
			}
			
			if (a.getLocation().getOwner() == null || (a.getLocation().getOwner() != a.originator && a.originator.relation(a.getLocation().getOwner()) == Diplomacy.Outcome.PEACE)) {
				// Exploration
				StringBuilder rep = new StringBuilder();
				boolean major = false;
				rep.append("An expedition led by ").append(a.name).append(" explores ").append(a.getLocation().name).append(". ");
				
				// - find artefacts
				// - exterminate bad wildlife
				
				boolean runAway = false;
				lp: for (SpecialLifeform slf : new ArrayList<SpecialLifeform>(a.getLocation().lifeforms)) {
					if (sg.coin()) { continue; }
					switch (slf) {
						case BRAIN_PARASITE:
						case ULTRAVORES:
						case SHAPE_SHIFTER:
							String monster = slf.name.toLowerCase();;
							major = true;
							rep.append("They encounter the local ").append(monster);
							if (sg.p(3)) {
								rep.append(" and exterminate them. ");
								a.getLocation().removeLifeform(slf);
							} else {
								if (a.fleet < 1) {
									rep.append(". In a desperate attempt to stop them, ").append(a.name).append(" activates the ship's self-destruct sequence.");
									runAway = true;
									sg.agents.remove(a);
									a.setLocation(null);
									break lp;
								} else {
									rep.append(". In a desperate attempt to stop them, ").append(a.name).append(" has half of the exploration fleet blasted to bits.");
									runAway = true;
									a.fleet /= 2;
									break lp;
								}
							}
							break;
					}
				}
				
				if (runAway) {
					sg.l(rep.toString());
					confirm();
					return;
				}
				
				// Inhabs
				if (!a.getLocation().inhabitants.isEmpty()) {
					major = true;
					rep.append("They trade with the local ").append(sg.pick(a.getLocation().inhabitants).type.name).append(". ");
					a.getLocation().evoPoints += 5000;
					a.resources += 2;
				}
				
				// Archeology!
				Planet p = a.getLocation();
				for (int stratNum = 0; stratNum < p.strata.size(); stratNum++) {
					Stratum stratum = p.strata.get(p.strata.size() - stratNum - 1);
					if (sg.p(4 + stratNum * 2)) {
						if (stratum instanceof Fossil) {
							rep.append("They discover: ").append(stratum.toString()).append(" ");
							a.originator.setScience(a.originator.getScience() + 1);
						}
						if (stratum instanceof Remnant) {
							rep.append("They discover: ").append(stratum.toString()).append(" ");
							a.resources++;
							Remnant r = (Remnant) stratum;
							Planet homeP = a.originator.largestColony();
							if (r.plague != null && sg.d(6) < r.plague.transmissivity) {
								boolean affects = false;
								for (Population pop : homeP.inhabitants) {
									if (r.plague.affects.contains(pop.type)) {
										affects = true;
									}
								}
								
								if (affects) {
									homeP.addPlague(new Plague(r.plague));
									rep.append(" Unfortunately, members of the expedition catch the ").append(r.plague.name).append(" from their exploration of the ancient tombs, infecting ").append(homeP.name).append(" upon their return. ");
									major = true;
								}
							}
						}
						if (stratum instanceof LostArtefact) {
							LostArtefact la = (LostArtefact) stratum;
							if (la.artefact.type == ArtefactType.PIRATE_TOMB || la.artefact.type == ArtefactType.PIRATE_HOARD || la.artefact.type == ArtefactType.ADVENTURER_TOMB) {
								rep.append("The expedition loots the ").append(la.artefact.desc).append(". ");
								a.resources += la.artefact.specialValue;
								p.strata.remove(stratum);
								stratNum--;
								continue;
							}
							if (la.artefact.type == ArtefactType.Device.STASIS_CAPSULE) {
								if (!sg.civs.contains(la.artefact.creator)) {
									rep.append("They open a stasis capsule from the ").append(la.artefact.creator.name).append(", which arises once more!");
									sg.civs.add(la.artefact.creator);
									la.artefact.creator.setTechLevel(la.artefact.creatorTechLevel);
									la.artefact.creator.setResources(10);
									la.artefact.creator.setMilitary(10);
									if (p.getOwner() != null) {
										p.getOwner().relations.put(la.artefact.creator, Diplomacy.Outcome.WAR);
										la.artefact.creator.relations.put(p.getOwner(), Diplomacy.Outcome.WAR);
									}
									p.setOwner(la.artefact.creator);
									boolean inserted = false;
									for (Population pop : p.inhabitants) {
										if (pop.type == la.artefact.st) {
											pop.setSize(pop.getSize() + 3);
											inserted = true;
											break;
										}
									}
									if (!inserted) {
										new Population(la.artefact.st, 3, p);
									}
									la.artefact.creator.birthYear = sg.year;
									p.strata.remove(stratum);
									stratNum--;
									break;
								}
								continue;
							}
							if (la.artefact.type == ArtefactType.Device.MIND_ARCHIVE) {
								rep.append("They encounter a mind archive of the ").append(la.artefact.creator.name).append(" which brings new knowledge and wisdom to the ").append(a.originator.name).append(". ");
								major = true;
								a.originator.setTechLevel(Math.max(a.originator.getTechLevel(), la.artefact.creatorTechLevel));
								continue;
							}
							if (la.artefact.type == ArtefactType.WRECK) {
								rep.append("They recover: ").append(stratum).append(" ");
								p.strata.remove(stratum);
								a.resources += 3;
								stratNum--;
								continue;
							}
							
							rep.append("They recover: ").append(stratum).append(" ");
							major = true;
							p.strata.remove(stratum);
							a.resources++;
							sg.pick(a.originator.getColonies()).addArtefact(la.artefact);
							stratNum--;
						}
					}
				}
				
				if (rep.length() > 0) {
					sg.l(rep.toString());
					confirm();
					return;
				}
				
				return;
			}
			
			if (a.getLocation().getOwner() == a.originator) {
				while (a.resources > 4) {
					a.fleet++;
					a.resources -= 4;
				}
				// Missions!
				// KILL PIRATE
				Agent pir = null;
				lp: for (Planet p : sg.planets) {
					for (Agent ag : sg.agents) {
						if (ag.type != AgentType.PIRATE) { continue; }
						if (ag.getLocation() == p) {
							pir = ag;
							break lp;
						}
					}
				}
				if (pir != null) {
					sg.l(a.name + " is sent on a mission to defeat the pirate " + pir.name + " by the government of " + a.getLocation().name + ".");
					if (sg.coin()) {
						sg.l(a.name + " fails to find any trace of the pirate " + pir.name + ".");
						return;
					}
					a.setLocation(pir.getLocation());
					sg.l(a.name + " tracks down the pirate " + pir.name + " in orbit around " + pir.getLocation().name + ".");
					// FAIGHTH!
					int attack = a.fleet * 4;
					int defence = pir.fleet * 3;
					int attackRoll = sg.d(attack, 6);
					int defenceRoll = sg.d(defence, 6);
					if (attackRoll > defenceRoll) {
						sg.l(a.name + " defeats the pirate " + pir.name + " - the skies of " + a.getLocation().name + " are safe again.");
						sg.agents.remove(pir);
						pir.setLocation(null);
						a.resources += pir.resources / 2;
						a.originator.setResources(a.originator.getResources() + pir.resources / 2);
					} else {
						if (a.fleet < 2) {
							sg.l(a.name + " is defeated utterly by the pirate.");
							sg.agents.remove(a);
							a.setLocation(null);
						} else {
							sg.l(a.name + " is defeated by the pirate " + pir.name + " and flees back to " + a.getLocation().name + ".");
							a.fleet /= 2;
						}
					}
					confirm();
					return;
				}
				
				// KILL SM
				Agent mon = null;
				lp: for (Planet p : sg.planets) {
					for (Agent ag : sg.agents) {
						if (ag.type != AgentType.SPACE_MONSTER) { continue; }
						if (ag.getLocation() == p) {
							mon = ag;
							break lp;
						}
					}
				}
				if (mon != null) {
					sg.l(a.name + " is sent on a mission to defeat the " + mon.name + " at " + mon.getLocation().name + ".");
					a.setLocation(mon.getLocation());
					encounter(a, sg, mon);
					return;
				}
				
				Agent ai = null;
				lp: for (Planet p : sg.planets) {
					for (Agent ag : sg.agents) {
						if (ag.type != AgentType.ROGUE_AI) { continue; }
						if (ag.getLocation() == p) {
							ai = ag;
							break lp;
						}
					}
				}
				if (ai != null) {
					sg.l(a.name + " is sent on a mission to stop the rogue AI " + ai.name + " at " + ai.getLocation().name + ".");
					a.setLocation(ai.getLocation());
					encounter(a, sg, ai);
					return;
				}
				
				Agent pr = null;
				lp: for (Planet p : sg.planets) {
					for (Agent ag : sg.agents) {
						if (ag.type != AgentType.SPACE_PROBE) { continue; }
						if (ag.getLocation() == p) {
							pr = ag;
							break lp;
						}
					}
				}
				if (ai != null) {
					sg.l(a.name + " is sent on a mission to stop the insane space probe " + pr.name + " threatening " + pr.getLocation().name + ".");
					a.setLocation(pr.getLocation());
					encounter(a, sg, pr);
					return;
				}
				
				// PEACE MISSION
				Civ enemy = null;
				for (Civ c : sg.civs) {
					if (c != a.originator && a.originator.relation(c) == Diplomacy.Outcome.WAR) {
						enemy = c;
					}
				}
				if (enemy != null && sg.p(4)) {
					sg.l("The " + a.originator.name + " send " + a.name + " on a mission of peace to the " + enemy.name + ".");
					a.setLocation(enemy.largestColony());
					if (sg.coin()) {
						sg.l("The expert diplomacy of " + a.name + " is successful: the two empires are at peace.");
						a.originator.relations.put(enemy, Diplomacy.Outcome.PEACE);
						enemy.relations.put(a.originator, Diplomacy.Outcome.PEACE);
					} else {
						a.setLocation(a.originator.largestColony());
						sg.l("Unfortunately, the peace mission fails. " + a.name + " hastily retreats to " + a.getLocation().name + ".");
					}
					confirm();
					return;
				}
				
				// Steal U
				if (enemy != null) {
					for (Planet p : enemy.getColonies()) {
						if (!p.artefacts.isEmpty()) {
							Artefact art = p.artefacts.get(0);
							sg.l("The " + a.originator.name + " send " + a.name + " on a mission to steal the " + art.type.getName() + " on " + p.name + ".");
							a.setLocation(p);
							if (sg.coin()) {
								Planet lc = a.originator.largestColony();
								sg.l(a.name + " successfully acquires the " + art.type.getName() + " and delivers it to " + lc.name + ".");
								/*p.artefacts.remove(art);
								lc.artefacts.add(art);*/
								p.moveArtefact(art, lc);
							} else {
								if (sg.p(3)) {
									sg.l("The " + enemy.name + " capture and execute " + a.name + " for trying to steal the " + art.type.getName() + ".");
									sg.agents.remove(a);
									a.setLocation(null);
								} else {
									a.setLocation(a.originator.largestColony());
									sg.l("The attempt to steal the " + art.type.getName() + " fails, and " + a.name + " swiftly retreats to " + a.getLocation().name + " to avoid capture.");
								}
							}
							confirm();
							return;
						}
					}
				}
			}
		}
	},
	SHAPE_SHIFTER() {
		@Override
		public String describe(Agent a, SpaceGen sg) {
			return "A pack of shape-shifters hiding amongst the local population.";
		}
		@Override
		public void behave(Agent a, SpaceGen sg) {
			if (a.getLocation().inhabitants.isEmpty()) {
				sg.agents.remove(a);
				a.setLocation(null);
				return;
			}
			if (a.getLocation().population() > 1) {
				if (sg.p(6)) {
					Population victim = sg.pick(a.getLocation().inhabitants);
					if (victim.getSize() == 1) {
						sg.l("Shape-shifters devour the last remaining " + victim.type.getName() + " on " + a.getLocation().name + ".");
						a.getLocation().dePop(victim, sg.year, null, "through predation by shape-shifters", null);
						confirm();
					} else {
						victim.setSize(victim.getSize() - 1);
					}
				}
				if (sg.p(40)) {
					sg.l("The inhabitants of " + a.getLocation().name + " manage to identify the shape-shifters among them and exterminate them.");
					sg.agents.remove(a);
					a.setLocation(null);
					confirm();
				}
			} else {
				sg.l("The population of " + a.getLocation().name + " turn out to be all shape-shifters. The colony collapses as the shape-shifters need real sentients to keep up their mimicry.");
				a.getLocation().deCiv(sg.year, null, "when the entire population of the planet turned out to be shape-shifters");
				if (!a.getLocation().lifeforms.contains(SpecialLifeform.SHAPE_SHIFTER)) {
					a.getLocation().addLifeform(SpecialLifeform.SHAPE_SHIFTER);
				}
				sg.agents.remove(a);
				a.setLocation(null);
				confirm();
			}
		}
	},
	ULTRAVORES() {
		@Override
		public String describe(Agent a, SpaceGen sg) {
			return "A pack of ultravores, incredibly dangerous predators.";
		}
		@Override
		public void behave(Agent a, SpaceGen sg) {
			if (a.getLocation().inhabitants.isEmpty() || a.getLocation().getOwner() == null) {
				sg.agents.remove(a);
				a.setLocation(null);
				return;
			}
			if (sg.p(6)) {
				if (a.getLocation().population() > 1) {
					Population victim = sg.pick(a.getLocation().inhabitants);
					if (victim.getSize() == 1) {
						sg.l("A billion " + victim.type.getName() + " on " + a.getLocation().name + " are devoured by ultravores.");
						a.getLocation().dePop(victim, sg.year, null, "through predation by ultravores", null);
					} else {
						victim.setSize(victim.getSize() - 1);
					}
				} else {
					sg.l("Ultravores devour the final inhabitants of " + a.getLocation().name + ".");
					a.getLocation().deCiv(sg.year, null, "through predation by ultravores");
					confirm();
				}
				if (sg.p(3) && a.getLocation().getOwner() != null) {
					lp: for (Planet p : a.getLocation().getOwner().fullColonies()) {
						for (Agent ag : sg.agents) {
							if (ag.type == AgentType.ULTRAVORES && ag.getLocation() == p) { continue lp; }
						}
						Agent ag = new Agent(ULTRAVORES, sg.year, "Hunting pack of Ultravores", sg);
						ag.setLocation(p);
						sg.agents.add(ag);
						break;
					}
				}
			}
		}
	},
	SPACE_MONSTER() {
		@Override
		public String describe(Agent a, SpaceGen sg) {
			return "In orbit: A " + a.name + " threatening the planet.";
		}
		@Override
		public void behave(Agent a, SpaceGen sg) {
			if (sg.p(500)) {
				sg.l("The " + a.name + " devours all life on " + a.getLocation().name + ".");
				a.getLocation().deLive(sg.year, null, "due to the attack of a " + a.name);
				return;
			}
			if (sg.p(8) && a.getLocation().population() > 2) {
				Population t = sg.pick(a.getLocation().inhabitants);
				if (t.getSize() == 1) {
					sg.l("The " + a.name + " devours the last of the local " + t.type.name + " on " + a.getLocation().name + ".");
					a.getLocation().dePop(t, sg.year, null, "due to predation by a " + a.name, null);
					confirm();
				} else {
					sg.l("The " + a.name + " devours one billion " + t.type.name + " on " + a.getLocation().name + ".");
					t.setSize(t.getSize() - 1);
				}
				return;
			}
			if (sg.p(20)) {
				sg.l("The " + a.name + " leaves the orbit of " + a.getLocation().name + " and heads back into deep space.");
				sg.agents.remove(a);
				a.setLocation(null);
				confirm();
				return;
			}
		}
	},
	SPACE_PROBE() {
		@Override
		public String describe(Agent a, SpaceGen sg) {
			return "In orbit: The insane space probe " + a.name + " threatening the planet.";
		}
		@Override
		public void behave(Agent a, SpaceGen sg) {
			if (a.getLocation() == null) {
				a.timer--;
				if (a.timer == 0) {
					a.setLocation(a.target);
					sg.l("The space probe " + a.name + " returns to " + a.getLocation().name + ".");
					if (a.getLocation().getOwner() == a.originator) {
						sg.l("The " + a.originator.name + " gains a wealth of new knowledge as a result.");
						a.originator.setTechLevel(a.originator.getTechLevel() + 3);
						sg.agents.remove(a);
						a.setLocation(null);
						return;
					} else {
						sg.l("Unable to contact the " + a.originator.name + " that launched it, the probe goes insane.");
						confirm();
					}
				}
				return;
			}
			if (sg.p(8) && a.getLocation().population() > 2) {
				Population t = sg.pick(a.getLocation().inhabitants);
				if (t.getSize() == 1) {
					sg.l("The insane space probe " + a.name + " bombards " + a.getLocation().name + ", wiping out the local " + t.type.name + ".");
					a.getLocation().dePop(t, sg.year, null, "due to bombardment by the insane space probe " + a.name, null);
				} else {
					sg.l("The insane space probe " + a.name + " bombards " + a.getLocation().name + ", killing one billion " + t.type.name + ".");
					t.setSize(t.getSize() - 1);
				}
				return;
			}
			if (sg.p(40)) {
				sg.l("The insane space probe " + a.name + " crashes into " + a.getLocation().name + ", wiping out all life on the planet.");
				a.getLocation().deLive(sg.year, null, "due to the impact of the space probe " + a.name);
				sg.agents.remove(a);
				a.setLocation(null);
				confirm();
				return;
			}
		}
	},
	ROGUE_AI() {
		@Override
		public String describe(Agent a, SpaceGen sg) {
			return "In orbit: The rogue AI " + a.name + ".";
		}
		@Override
		public void behave(Agent a, SpaceGen sg) {
			if (a.timer > 0) {
				a.timer--;
				if (a.timer == 0) {
					a.setLocation(sg.pick(sg.planets));
					sg.l("The rogue AI " + a.name + " reappears on " + a.getLocation().name + ".");
				}
				return;
			}
			if (sg.p(10)) {
				a.setLocation(sg.pick(sg.planets));
			}
			if (sg.p(50)) {
				sg.l("The rogue AI " + a.name + " vanishes without a trace.");
				a.timer = 40 + sg.d(500); // originally 500
				a.setLocation(null);
				return;
			}
			if (sg.p(80 +( a.Intelligence * 10))) {
				sg.l("The rogue AI " + a.name + " vanishes without a trace.");
				sg.agents.remove(a);
				a.setLocation(null);
				return;
			}
			
			if (sg.p(40)) {
				for (Agent ag : sg.agents) {
					if (ag == a) { continue; }
					if (ag.getLocation() != a.getLocation()) { continue; }
					Artefact art = null;
					if(a.getLocation() != null){
					switch (ag.type) {
						case ADVENTURER:
							sg.l("The rogue AI " + a.name + " encases the adventurer " + ag.name + " in a block of time ice.");
							art = new Artefact(sg.year, "the rogue AI " + a.name, ArtefactType.TIME_ICE,
									"block of time ice encasing " + ag.name);
							break;
						case PIRATE:
							sg.l("The rogue AI " + a.name + " encases the pirate " + ag.name + " in a block of time ice.");
							art = new Artefact(sg.year, "the rogue AI " + a.name, ArtefactType.TIME_ICE,
									"block of time ice encasing the pirate " + ag.name);
							break;
						case SHAPE_SHIFTER:
							sg.l("The rogue AI " + a.name + " encases the shape-shifters on" + a.getLocation().name + " in a block of time ice.");
							art = new Artefact(sg.year, "the rogue AI " + a.name, ArtefactType.TIME_ICE,
									"block of time ice, encasing a group of shape-shifters");
							break;
						case ULTRAVORES:
							sg.l("The rogue AI " + a.name + " encases a pack of ultravores on " + a.getLocation().name + " in a block of time ice.");
							art = new Artefact(sg.year, "the rogue AI " + a.name, ArtefactType.TIME_ICE,
									"block of time ice, encasing a pack of ultravores");
							break;
						case ROGUE_AI:
							String newName = "Cluster " + sg.r.nextInt(100);
							sg.l("The rogue AI " + a.name + " merges with the rogue AI " + a.getLocation().name + " into a new entity called " + newName + ".");
							a.name = newName;
							sg.agents.remove(ag);
							a.setLocation(null);
							confirm();
							return;
					}
				}
					if (art != null) {
						art.containedAgent = ag;
						a.getLocation().addArtefact(art);
						sg.agents.remove(ag);
						a.setLocation(null);
						confirm();
						return;
					}
				}
			}
		if(a != null){
		if(a.getLocation() != null)	{
			// Random mischief!
			if (a.getLocation().getOwner() != null) {
				if (sg.p(60)) {
					SentientType st = sg.pick(a.getLocation().getOwner().fullMembers);
					String name = sg.pick(st.base.nameStarts) + sg.pick(st.base.nameEnds);
					String title = null;
					switch (a.getLocation().getOwner().getGovt()) {
						case DICTATORSHIP: title = "Emperor"; break;
						case FEUDAL_STATE: title = "King"; break;
						case REPUBLIC: title = "President"; break;
						case THEOCRACY: title = "Autarch"; break;
					}
					Artefact ar = new Artefact(sg.year, "the rogue AI " + a.name, ArtefactType.TIME_ICE,
									"block of time ice, encasing " + name + ", " + title + " of the " + a.getLocation().getOwner().name);
					ar.containedST = st;
					a.getLocation().addArtefact(ar);
					sg.l("The rogue AI " + a.name + " encases " + name + ", " + title + " of the " + a.getLocation().getOwner().name + ", in a block of time ice.");
					confirm();
					return;
				}
				if (sg.p(60)) {
					sg.l("The rogue AI " + a.name + " crashes the " + a.getLocation().name + " stock exchange.");
					a.getLocation().getOwner().setResources(a.getLocation().getOwner().getResources() / 2);
					return;
				}
				if (sg.p(30)) {
					ArtefactType.Device dt = sg.pick(ArtefactType.Device.values());
					if (dt == ArtefactType.Device.STASIS_CAPSULE) { return; }
					if (dt == ArtefactType.Device.MIND_ARCHIVE) { return; }
					Artefact dev = new Artefact(sg.year, "the rogue AI " + a.name, dt, dt.create(null, sg));
					a.getLocation().addArtefact(dev);
					sg.l("The rogue AI " + a.name + " presents the inhabitants of " + a.getLocation().name + " with a gift: a " + dev.type.getName() + ".");
					confirm();
					return;
				}
				if (sg.p(20) && !a.getLocation().artefacts.isEmpty()) {
					Artefact art = sg.pick(a.getLocation().artefacts);
					Planet t = sg.pick(sg.planets);
					sg.l("The rogue AI " + a.name + " steals the " + art.desc + " on " + a.getLocation().name + " and hides it on " + t.name + ".");
					//a.p.artefacts.remove(art);
					// qqDPS
					a.getLocation().removeArtefact(art);
					t.strata.add(new LostArtefact("hidden", sg.year, art));
					confirm();
					return;
				}
			}




			if (!a.getLocation().inhabitants.isEmpty()) {
				if (sg.p(40)) {
					Plague pl = new Plague(sg);
					pl.affects.add(a.getLocation().inhabitants.get(0).type);
					for (int i = 1; i < a.getLocation().inhabitants.size(); i++) {
						if (sg.coin()) {
							pl.affects.add(a.getLocation().inhabitants.get(i).type);
						}
					}
					a.getLocation().addPlague(pl);
					sg.l("The rogue AI " + a.name + " infects the inhabitants of " + a.getLocation().name + " with " + pl.desc() + ".");
					confirm();
					return;
				}
				if (a.getLocation().population() > 2 && sg.p(25)) {
					for (Planet t : sg.planets) {
						if (t.habitable && t.getOwner() == null) {
							Population victim = sg.pick(a.getLocation().inhabitants);
							victim.send(t);
							sg.l("The rogue AI " + a.name + " abducts a billion " + victim.type.name + " from " + a.getLocation().name + " and dumps them on " + t.name + ".");
							confirm();
							return;
						}
					}
				}
			}
			
			if (a.getLocation().habitable && sg.p(200) && a.getLocation().getOwner() == null) {
				SentientType st = SentientType.invent(sg, null, a.getLocation(), "They were created by the rogue AI " + a.name + " in " + sg.year + ".");
				sg.l("The rogue AI " + a.name + " uplifts the local " + st.name + " on " + a.getLocation().name + ".");
				new Population(st, 3 + sg.d(3), a.getLocation());
				confirm();
				return;
			}
			
			if (a.getLocation().habitable && sg.p(250) && a.getLocation().getOwner() == null) {
				SentientType st = SentientType.genRobots(sg, null, a.getLocation(), "They were created by the rogue AI " + a.name + " in " + sg.year + ".");
				sg.l("The rogue AI " + a.name + " creates " + st.name + " on " + a.getLocation().name + ".");
				new Population(st, 3 + sg.d(3), a.getLocation());
				confirm();
				return;
			}
			
			if (!a.getLocation().habitable && sg.p(500)) {
				sg.l("The rogue AI " + a.name + " terraforms " + a.getLocation().name + ".");
				confirm();
				return;
			}
			
			if (sg.p(300) && a.getLocation().habitable) {
				sg.l("The rogue AI " + a.name + " releases nanospores on " + a.getLocation().name + ", destroying all life on the planet.");
				a.getLocation().deLive(sg.year, null, "due to nanospores relased by " + a.name);
				confirm();
				return;
			}
			
			if (sg.civs.size() > 1 && sg.p(250)) {
				Civ c = sg.pick(sg.civs);
				ArrayList<Civ> others = new ArrayList<Civ>(sg.civs);
				others.remove(c);
				Civ c2 = sg.pick(others);
				if (c.relation(c2) == Diplomacy.Outcome.PEACE) {
					sg.l("The rogue AI " + a.name + " incites war between the " + c.name + " and the " + c2.name + ".");
					c.relations.put(c2, Diplomacy.Outcome.WAR);
					c2.relations.put(c, Diplomacy.Outcome.WAR);
				} else {
					sg.l("The rogue AI " + a.name + " brokers peace between the " + c.name + " and the " + c2.name + ".");
					c.relations.put(c2, Diplomacy.Outcome.PEACE);
					c2.relations.put(c, Diplomacy.Outcome.PEACE);
				}
				confirm();
			}
		}
	}}
	}/*,
	ADVENTURER,
	MIMIC,
	ULTRAVORE,
	SPACE_MONSTER*/;
	
	public static final String[] MONSTER_TYPES = { "worm", "cube", "crystal", "jellyfish" };
	
	public abstract void behave(Agent a, SpaceGen sg);
	
	public abstract String describe(Agent a, SpaceGen sg);
}
