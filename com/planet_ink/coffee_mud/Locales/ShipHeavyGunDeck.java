package com.planet_ink.coffee_mud.Locales;
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
   Copyright 2019-2019 Bo Zimmerman

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
public class ShipHeavyGunDeck extends WoodenDeck
{
	@Override
	public String ID()
	{
		return "ShipHeavyGunDeck";
	}

	protected Ability capacityA;

	public ShipHeavyGunDeck()
	{
		super();
		name="the ship deck";
		capacityA=CMClass.getAbility("Prop_ReqCapacity");
		if(capacityA!=null)
		{
			capacityA.setMiscText("items=20 weight=1000 siegeweapons=3");
			capacityA.setAffectedOne(this);
			capacityA.makeNonUninvokable();
		}
		recoverPhyStats();
	}

	@Override
	public CMObject copyOf()
	{
		final ShipHeavyGunDeck R = (ShipHeavyGunDeck)super.copyOf();
		R.capacityA=CMClass.getAbility("Prop_ReqCapacity");
		R.capacityA.setMiscText("items=20 weight=1000 siegeweapons=3");
		R.capacityA.setAffectedOne(R);
		R.capacityA.makeNonUninvokable();
		return R;
	}

	@Override
	public CMObject newInstance()
	{
		final ShipHeavyGunDeck R = (ShipHeavyGunDeck)super.newInstance();
		R.capacityA=CMClass.getAbility("Prop_ReqCapacity");
		R.capacityA.setMiscText("items=20 weight=1000 siegeweapons=3");
		R.capacityA.setAffectedOne(R);
		R.capacityA.makeNonUninvokable();
		return R;
	}

	@Override
	public boolean okMessage(final Environmental myHost, final CMMsg msg)
	{
		if((capacityA!=null)&&(!capacityA.okMessage(myHost, msg)))
			return false;
		return super.okMessage(myHost, msg);
	}
}
