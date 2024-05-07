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
import static com.zarkonnen.spacegen.Stage.change;
import static com.zarkonnen.spacegen.Stage.delay;
import static com.zarkonnen.spacegen.Stage.tracking;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import com.zarkonnen.spacegen.ArtefactType.Device;
import com.zarkonnen.spacegen.SentientType.Base;
import java.lang.reflect.Array;

public class Civ {
	ArrayList<SentientType> fullMembers = new ArrayList<SentientType>();
	private Government govt;
	HashMap<Civ, Diplomacy.Outcome> relations = new HashMap<Civ, Diplomacy.Outcome>();
	int number = 0;

	ArrayList<CivSprite> sprites = new ArrayList<CivSprite>();

	// these stats should just be modifiers
	int Strength = 0;
	int Dexterity = 0;
	int Constitution = 0;
	int Intelligence = 0;
	int Charisma = 0;

	int civStrength = 0;
	int civDexterity = 0;
	int civConstitution = 0;
	int civIntelligence = 0;
	int civCharisma = 0;

	int racesStrength = -3;
	int racesDexterity = -3;
	int racesConstitution = -3;
	int racesIntelligence = -3;
	int racesCharisma = -3;


	private int resources = 0;
	private int science = 0;
	private int military = 0;
	private int weapLevel = 0;
	private int techLevel = 0;
	String name;
	int birthYear;
	int nextBreakthrough = 6;
	int decrepitude = 0;
	private final SpaceGen sg;

	public ArrayList<Planet> getColonies() {
		ArrayList<Planet> cols = new ArrayList<Planet>();
		for (Planet p : sg.planets) {
			if (p.getOwner() == this) {
				cols.add(p);
			}
		}
		return cols;
	}

	public int getResources() {
		return resources;
	}

	public final void setResources(int resources) {
		resources = Math.max(0, resources);
		int oldRes = this.resources;
		this.resources = resources;
		for (CivSprite cs : sprites) {
			cs.changeRes(oldRes, resources);
		}
		animate();
	}

	public int getScience() {
		return science;
	}

	public void setScience(int science) {
		science = Math.max(0, science);
		int oldSci = this.science;
		this.science = science;
		for (CivSprite cs : sprites) {
			cs.changeScience(oldSci, science);
		}
		animate();
	}

	public int getMilitary() {
		return military;
	}

	public void setMilitary(int military) {
		military = Math.max(0, military);
		int oldMil = this.military;
		this.military = military;
		for (CivSprite cs : sprites) {
			cs.changeFleet(oldMil, military);
		}
		animate();
	}

	public int getTechLevel() {
		return techLevel;
	}

	public final void setTechLevel(int techLevel) {
		techLevel = Math.max(0, techLevel);
		this.techLevel = techLevel;
		for (CivSprite cs : sprites) {
			cs.changeTech(techLevel);
		}
		animate();
	}

	public int getWeapLevel() {
		return weapLevel;
	}

	public void setWeapLevel(int weapLevel) {
		weapLevel = Math.max(0, weapLevel);
		this.weapLevel = weapLevel;
		for (CivSprite cs : sprites) {
			cs.changeMilTech(weapLevel);
		}
		animate();
	}

	Planet leastPopulousFullColony() {
		Planet c = null;
		int pop = 0;
		for (Planet p : fullColonies()) {
			if (c == null || p.population() < pop) {
				c = p;
				pop = p.population();
			}
		}
		return c;
	}

	public Planet closestColony(Planet p) {
		Planet c = null;
		int closestDist = 0;
		for (Planet col : fullColonies()) {
			int dist = (p.x - col.x) * (p.x - col.x) + (p.y - col.y) * (p.y - col.y);
			if (dist < closestDist || c == null) {
				c = col;
				closestDist = dist;
			}
		}
		if (c == null) {
			return getColonies().get(0);
		} else {
			return c;
		}
	}

	public ArrayList<Planet> reachables(SpaceGen sg) {
		int range = 3 + getTechLevel() * getTechLevel();
		if (has(ArtefactType.Device.TELEPORT_GATE)) {
			range = 10000;
		}
		ArrayList<Planet> ir = new ArrayList<Planet>();
		for (Planet p : sg.planets) {
			int closestR = 100000;
			for (Planet c : getColonies()) {
				int dist = (p.x - c.x) * (p.x - c.x) + (p.y - c.y) * (p.y - c.y);
				closestR = Math.min(dist, closestR);
			}
			if (closestR <= range) {
				ir.add(p);
			}
		}
		return ir;
	}

	public boolean has(ArtefactType at) {
		for (Planet c : getColonies()) {
			for (Artefact a : c.artefacts) {
				if (a.type == at) {
					return true;
				}
			}
		}
		return false;
	}

	Artefact use(ArtefactType at) {
		for (Planet c : getColonies()) {
			for (Artefact a : c.artefacts) {
				if (a.type == at) {
					c.artefacts.remove(a);
					return a;
				}
			}
		}
		return new Artefact(13, this, at, "mysterious " + at.getName() + "");
	}

	public Diplomacy.Outcome relation(Civ c) {
		if (!relations.containsKey(c)) {
			relations.put(c, Diplomacy.Outcome.PEACE);
		}
		return relations.get(c);
	}


	public void calculate_race_stats(){
		for( int x = 0; x < fullMembers.size(); x++ ){
			if(fullMembers.get(x).Strength>racesStrength) {
				racesStrength = fullMembers.get(x).Strength;
			}
			if(fullMembers.get(x).Dexterity>racesDexterity) {
				racesDexterity = fullMembers.get(x).Dexterity;
			}
			if(fullMembers.get(x).Charisma>racesCharisma) {
				racesCharisma = fullMembers.get(x).Charisma;
			}
			if(fullMembers.get(x).Intelligence>racesIntelligence) {
				racesIntelligence = fullMembers.get(x).Intelligence;
			}
			if(fullMembers.get(x).Constitution>racesConstitution) {
				racesConstitution = fullMembers.get(x).Constitution;
			}
		}
		//System.out.println("Race stats calculated \nSTR,Dex,Cha,Int,Con " + "\n	" + racesStrength+ " " +racesDexterity + " " + racesCharisma+ " " + racesIntelligence + " "+ racesConstitution);
	}

	public Civ(int year, SentientType st, Planet home, Government govt, int resources, SpaceGen sg) {
		this.govt = govt;
		this.sg = sg;
		if (st != null) {
			this.fullMembers.add(st);
		}
		calculate_race_stats();
		setResources(resources);
		this.birthYear = year;
		updateName(sg.historicalCivNames);
		home.setOwner(this);
		setTechLevel(1);
		civStrength = returnMod(SpaceGen.d(2, 10)+racesStrength+2);
		civDexterity = returnMod(SpaceGen.d(2, 10)+racesDexterity+2);
		civConstitution = returnMod(SpaceGen.d(2, 10)+racesConstitution+2);
		civIntelligence = returnMod(SpaceGen.d(4, 5)+racesIntelligence+4);
		civCharisma = returnMod(SpaceGen.d(2, 10)+racesCharisma+2);
		Strength = civStrength;
		Dexterity = civDexterity;
		Constitution = civConstitution;
		Intelligence = civIntelligence;
		Charisma = civCharisma;
		//calculate_race_stats();
	}

	public int getCharisma() {
		return Charisma + racesCharisma;
	}

	public int getIntelligence() {
		return Intelligence + racesIntelligence;
	}

	public int getConstitution() {
		return Constitution + racesConstitution;
	}

	public int getDexterity() {
		return Dexterity + racesDexterity;
	}

	public int getStrength() {
		return Strength + racesStrength;
	}

	private int returnMod(int x) {
		int mod = 0;
		if (x < 4)
			mod = -3;
		else if (x < 6)
			mod = -2;
		else if (x < 10)
			mod = -1;
		else if (x < 12)
			mod = 0;
		else if (x < 16)
			mod = 1;
		else if (x < 20)
			mod = 2;
		else
			mod = 3;
		return mod;
	}

	public int population() {
		int sum = 0;
		for (Planet col : getColonies()) {
			sum += col.population();
		}
		return sum;
	}

	public ArrayList<Planet> fullColonies() {
		ArrayList<Planet> fcs = new ArrayList<Planet>();
		for (Planet col : getColonies()) {
			if (col.population() > 0) {
				fcs.add(col);
			}
		}
		return fcs;
	}

	public Planet largestColony() {
		int sz = -1;
		Planet largest = null;
		for (Planet col : getColonies()) {
			if (col.population() > sz) {
				largest = col;
				sz = col.population();
			}
		}
		return largest;
	}

	final String genName(int nth) {
		String n = "";
		if (nth > 1) {
			n = Names.nth(nth) + " ";
		}
		n += getGovt().title + " of ";
		if (fullMembers.size() == 1) {
			n += fullMembers.get(0).getName();
		} else {
			HashSet<SentientType.Base> bases = new HashSet<SentientType.Base>();
			for (SentientType st : fullMembers) {
				bases.add(st.base);
			}
			ArrayList<SentientType.Base> bs = new ArrayList<SentientType.Base>(bases);
			for (int i = 0; i < bs.size(); i++) {
				if (i > 0) {
					if (i == bs.size() - 1) {
						n += " and ";
					} else {
						n += ", ";
					}
				}
				n += bs.get(i).name;
			}
		}
		return n;
	}

	final void updateName(ArrayList<String> historicals) {
		number = 0;
		while (true) {
			number++;
			String n = genName(number);
			if (historicals.contains(n)) {
				continue;
			}
			name = n;
			break;
		}
		historicals.add(name);
	}

	public String fullDesc(SpaceGen sg) {
		StringBuilder sb = new StringBuilder();
		sb.append("THE ").append(name.toUpperCase()).append(":\n");
		int age = sg.year - birthYear;
		if (age < 3) {
			sb.append("A recently emerged");
		} else if (age < 8) {
			sb.append("A young");
		} else if (age < 16) {
			sb.append("A well-established");
		} else {
			sb.append("An ancient");
		}

		if (decrepitude >= 20) {
			sb.append(", corrupt");
		} else if (decrepitude >= 40) {
			sb.append(", crumbling");
		}

		if (getResources() < 2) {
			sb.append(", dirt poor");
		} else if (getResources() < 4) {
			sb.append(", impoverished");
		} else if (getResources() < 16) {

		} else if (getResources() < 25) {
			sb.append(", wealthy");
		} else {
			sb.append(", fantastically wealthy");
		}

		if (getTechLevel() < 2) {
			sb.append(", primitive");
		} else if (getTechLevel() < 4) {

		} else if (getTechLevel() < 7) {
			sb.append(", advanced");
		} else {
			sb.append(", highly advanced");
		}

		sb.append(" ").append(getGovt().title).append(" of ");
		if (getColonies().size() == 1) {
			sb.append("a single planet, ").append(getColonies().get(0).name);
		} else {
			sb.append(getColonies().size()).append(" planets");
		}
		sb.append(", with ").append(population()).append(" billion inhabitants.\n");
		sb.append("Major populations:\n");
		HashMap<SentientType, Integer> pops = new HashMap<SentientType, Integer>();
		for (Planet c : getColonies()) {
			for (Population pop : c.inhabitants) {
				if (!pops.containsKey(pop.type)) {
					pops.put(pop.type, pop.getSize());
				} else {
					pops.put(pop.type, pops.get(pop.type) + pop.getSize());
				}
			}
		}
		for (Map.Entry<SentientType, Integer> e : pops.entrySet()) {
			if (!fullMembers.contains(e.getKey())) {
				continue;
			}
			sb.append(e.getValue()).append(" billion ").append(e.getKey().getName()).append(".\n");
		}
		for (Map.Entry<SentientType, Integer> e : pops.entrySet()) {
			if (fullMembers.contains(e.getKey())) {
				continue;
			}
			sb.append(e.getValue()).append(" billion enslaved ").append(e.getKey().getName()).append(".\n");
		}
		HashSet<Device> devices = new HashSet<Device>();
		for (Planet c : getColonies()) {
			for (Artefact a : c.artefacts) {
				if (a.type instanceof Device) {
					devices.add((Device) a.type);
				}
			}
		}
		for (Device d : devices) {
			sb.append("It controls a ").append(d.getName()).append(".\n");
		}
		for (Civ other : sg.civs) {
			if (other == this) {
				continue;
			}
			if (relation(other) == Diplomacy.Outcome.WAR) {
				sb.append("It is at war with the ").append(other.name).append(".\n");
			} else {
				sb.append("It is at peace with the ").append(other.name).append(".\n");
			}
		}
		sb.append("\n	Their Stats are: STR: ").append(Strength).append(" DEX: " ).append(Dexterity).append(" CON: " ).append(Constitution).append(" INT: " ).append(Intelligence).append(" CHA: " ).append(Charisma);
		return sb.toString();
	}

	boolean has(Base base) {
		for (SentientType st : fullMembers) {
			if (st.base == base) {
				return true;
			}
		}
		return false;
	}

	public Government getGovt() {
		return govt;
	}

	void setGovt(Government govt, ArrayList<String> historicalNames) {
		this.govt = govt;
		updateName(historicalNames);
		animate(tracking(largestColony().sprite, delay()));
		for (CivSprite s : sprites) {
			add(change(s, Imager.get(this)));
		}
		animate();
	}
}
