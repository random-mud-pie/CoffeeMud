package com.planet_ink.coffee_mud.Races;

import com.planet_ink.coffee_mud.interfaces.*;
import com.planet_ink.coffee_mud.common.*;
import com.planet_ink.coffee_mud.utils.*;
import java.util.*;

/* 
   Copyright 2000-2004 Bo Zimmerman

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
public class Troll extends StdRace
{
	public String ID(){	return "Troll"; }
	public String name(){ return "Troll"; }
	public int shortestMale(){return 74;}
	public int shortestFemale(){return 70;}
	public int heightVariance(){return 14;}
	public int lightestWeight(){return 200;}
	public int weightVariance(){return 200;}
	public long forbiddenWornBits(){return 0;}
	public String racialCategory(){return "Troll-kin";}
	private String[]culturalAbilityNames={"Draconic"};
	private int[]culturalAbilityProfficiencies={50};
	public String[] culturalAbilityNames(){return culturalAbilityNames;}
	public int[] culturalAbilityProfficiencies(){return culturalAbilityProfficiencies;}

	//                                an ey ea he ne ar ha to le fo no gi mo wa ta wi
	private static final int[] parts={0 ,2 ,2 ,1 ,1 ,2 ,2 ,1 ,2 ,2 ,1 ,0 ,1 ,1 ,0 ,0 };
	public int[] bodyMask(){return parts;}

	private int[] agingChart={0,1,5,40,100,150,200,230,260};
	public int[] getAgingChart(){return agingChart;}
	
	protected static Vector resources=new Vector();
	public int availability(){return Race.AVAILABLE_MAGICONLY;}

	public void affectCharStats(MOB affectedMOB, CharStats affectableStats)
	{
		super.affectCharStats(affectedMOB, affectableStats);
		affectableStats.setPermaStat(CharStats.STRENGTH,16);
		affectableStats.setPermaStat(CharStats.DEXTERITY,12);
		affectableStats.setPermaStat(CharStats.INTELLIGENCE,8);
	}
	public String arriveStr()
	{
		return "thunders in";
	}
	public String leaveStr()
	{
		return "leaves";
	}
	public Weapon myNaturalWeapon()
	{
		if(naturalWeapon==null)
		{
			naturalWeapon=CMClass.getWeapon("StdWeapon");
			naturalWeapon.setName("huge clawed hands");
			naturalWeapon.setWeaponType(Weapon.TYPE_SLASHING);
		}
		return naturalWeapon;
	}
	public String healthText(MOB mob)
	{
		double pct=(Util.div(mob.curState().getHitPoints(),mob.maxState().getHitPoints()));

		if(pct<.10)
			return "^r" + mob.name() + "^r is near to heartless death!^N";
		else
		if(pct<.20)
			return "^r" + mob.name() + "^r is covered in torn slabs of flesh.^N";
		else
		if(pct<.30)
			return "^r" + mob.name() + "^r is gored badly with lots of tears.^N";
		else
		if(pct<.40)
			return "^y" + mob.name() + "^y has numerous gory tears and gashes.^N";
		else
		if(pct<.50)
			return "^y" + mob.name() + "^y has some gory tears and gashes.^N";
		else
		if(pct<.60)
			return "^p" + mob.name() + "^p has a few gory wounds.^N";
		else
		if(pct<.70)
			return "^p" + mob.name() + "^p is cut and bruised heavily.^N";
		else
		if(pct<.80)
			return "^g" + mob.name() + "^g has some minor cuts and bruises.^N";
		else
		if(pct<.90)
			return "^g" + mob.name() + "^g has a few bruises and scratches.^N";
		else
		if(pct<.99)
			return "^g" + mob.name() + "^g has a few small bruises.^N";
		else
			return "^c" + mob.name() + "^c is in perfect health.^N";
	}
	public Vector myResources()
	{
		synchronized(resources)
		{
			if(resources.size()==0)
			{
				for(int i=0;i<4;i++)
					resources.addElement(makeResource
					("a strip of "+name().toLowerCase()+" hide",EnvResource.RESOURCE_LEATHER));
				resources.addElement(makeResource
				("some "+name().toLowerCase()+" blood",EnvResource.RESOURCE_BLOOD));
				resources.addElement(makeResource
				("a pile of "+name().toLowerCase()+" bones",EnvResource.RESOURCE_BONE));
			}
		}
		return resources;
	}
}
