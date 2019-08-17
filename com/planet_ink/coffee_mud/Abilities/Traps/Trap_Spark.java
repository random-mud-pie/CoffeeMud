package com.planet_ink.coffee_mud.Abilities.Traps;
import com.planet_ink.coffee_mud.core.interfaces.*;
import com.planet_ink.coffee_mud.core.*;
import com.planet_ink.coffee_mud.core.collections.*;
import com.planet_ink.coffee_mud.Abilities.interfaces.*;
import com.planet_ink.coffee_mud.Areas.interfaces.*;
import com.planet_ink.coffee_mud.Behaviors.interfaces.*;
import com.planet_ink.coffee_mud.CharClasses.interfaces.*;
import com.planet_ink.coffee_mud.Commands.interfaces.*;
import com.planet_ink.coffee_mud.Common.interfaces.*;
import com.planet_ink.coffee_mud.Exits.interfaces.*;
import com.planet_ink.coffee_mud.Items.interfaces.*;
import com.planet_ink.coffee_mud.Libraries.interfaces.*;
import com.planet_ink.coffee_mud.Locales.interfaces.*;
import com.planet_ink.coffee_mud.MOBS.interfaces.*;
import com.planet_ink.coffee_mud.Races.interfaces.*;

import java.util.*;

/*
   Copyright 2003-2019 Bo Zimmerman

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
public class Trap_Spark extends StdTrap
{
	@Override
	public String ID()
	{
		return "Trap_Spark";
	}

	private final static String	localizedName	= CMLib.lang().L("sparking trap");

	@Override
	public String name()
	{
		return localizedName;
	}

	@Override
	protected int canAffectCode()
	{
		return Ability.CAN_ITEMS;
	}

	@Override
	protected int canTargetCode()
	{
		return 0;
	}

	@Override
	protected int trapLevel()
	{
		return 19;
	}

	@Override
	public String requiresToSet()
	{
		return "10 pounds of metal";
	}

	@Override
	public Trap setTrap(final MOB mob, final Physical P, final int trapBonus, final int qualifyingClassLevel, final boolean perm)
	{
		if(P==null)
			return null;
		if(mob!=null)
		{
			final RawMaterial I=findMostOfMaterial(mob.location(),RawMaterial.MATERIAL_METAL);
			if(I!=null)
				super.destroyResources(mob.location(),I.material(),I.getSubType(),10);
		}
		return super.setTrap(mob,P,trapBonus,qualifyingClassLevel,perm);
	}

	@Override
	public List<Item> getTrapComponents()
	{
		final List<Item> V=new Vector<Item>();
		for(int i=0;i<10;i++)
			V.add(CMLib.materials().makeItemResource(RawMaterial.RESOURCE_IRON));
		return V;
	}

	@Override
	public boolean canSetTrapOn(final MOB mob, final Physical P)
	{
		if(!super.canSetTrapOn(mob,P))
			return false;
		if(mob!=null)
		{
			final RawMaterial I=findMostOfMaterial(mob.location(),RawMaterial.MATERIAL_METAL);
			if((I==null)
			||(super.findNumberOfResource(mob.location(),I)<10))
			{
				mob.tell(L("You'll need to set down at least 10 pounds of metal first."));
				return false;
			}
		}
		return true;
	}

	@Override
	public void spring(final MOB target)
	{
		if((target!=invoker())&&(target.location()!=null))
		{
			if((!canInvokeTrapOn(invoker(),target))
			||(isLocalExempt(target))
			||(invoker().getGroupMembers(new HashSet<MOB>()).contains(target))
			||(target==invoker())
			||(doesSaveVsTraps(target)))
				target.location().show(target,null,null,CMMsg.MASK_ALWAYS|CMMsg.MSG_NOISE,L("<S-NAME> avoid(s) setting off a sparking trap!"));
			else
			if(target.location().show(target,target,this,CMMsg.MASK_ALWAYS|CMMsg.MSG_NOISE,L("<S-NAME> set(s) off an sparking trap!")))
			{
				super.spring(target);
				CMLib.combat().postDamage(invoker(),target,null,CMLib.dice().roll(trapLevel()+abilityCode(),8,1),CMMsg.MASK_ALWAYS|CMMsg.TYP_FIRE,Weapon.TYPE_BURNING,L("The sparks <DAMAGE> <T-NAME>!@x1",CMLib.protocol().msp("shock.wav",30)));
				if((canBeUninvoked())&&(affected instanceof Item))
					disable();
			}
		}
	}
}
