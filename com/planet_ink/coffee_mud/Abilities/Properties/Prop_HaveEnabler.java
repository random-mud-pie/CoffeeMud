package com.planet_ink.coffee_mud.Abilities.Properties;
import com.planet_ink.coffee_mud.core.interfaces.*;
import com.planet_ink.coffee_mud.core.*;
import com.planet_ink.coffee_mud.core.collections.*;
import com.planet_ink.coffee_mud.Abilities.SuperPowers.SuperPower;
import com.planet_ink.coffee_mud.Abilities.interfaces.*;
import com.planet_ink.coffee_mud.Areas.interfaces.*;
import com.planet_ink.coffee_mud.Behaviors.interfaces.*;
import com.planet_ink.coffee_mud.CharClasses.interfaces.*;
import com.planet_ink.coffee_mud.Commands.interfaces.*;
import com.planet_ink.coffee_mud.Common.interfaces.*;
import com.planet_ink.coffee_mud.Exits.interfaces.*;
import com.planet_ink.coffee_mud.Items.interfaces.*;
import com.planet_ink.coffee_mud.Libraries.interfaces.*;
import com.planet_ink.coffee_mud.Libraries.interfaces.MaskingLibrary.CompiledZMaskEntry;
import com.planet_ink.coffee_mud.Locales.interfaces.*;
import com.planet_ink.coffee_mud.MOBS.interfaces.*;
import com.planet_ink.coffee_mud.Races.interfaces.*;

import java.util.*;

/*
   Copyright 2004-2019 Bo Zimmerman

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
public class Prop_HaveEnabler extends Prop_SpellAdder
{
	@Override
	public String ID()
	{
		return "Prop_HaveEnabler";
	}

	@Override
	public String name()
	{
		return "Granting skills when owned";
	}

	@Override
	protected int canAffectCode()
	{
		return Ability.CAN_ITEMS;
	}

	protected Item				myItem			= null;
	protected List<String>		lastMOBeffects	= new SVector<String>();
	protected boolean			processing2		= false;
	protected volatile boolean	clearedYet		= false;

	@Override
	public long flags()
	{
		return Ability.FLAG_ENABLER;
	}

	@Override
	public int triggerMask()
	{
		return TriggeredAffect.TRIGGER_GET;
	}

	@Override
	public String accountForYourself()
	{
		return spellAccountingsWithMask("Grants ", " to the owner.");
	}

	@Override
	public void setMiscText(final String newText)
	{
		super.setMiscText(newText);
		lastMOBeffects=new Vector<String>();
	}

	public boolean addMeIfNeccessary(final Environmental source, final Environmental target, final short maxTicks)
	{
		if((!(target instanceof MOB))
		||((compiledMask!=null)&&(!CMLib.masking().maskCheck(compiledMask,target,true))))
			return false;
		final MOB newMOB=(MOB)target;
		final PairList<Ability, Integer> allAbles=getMySpellsV();
		int proff=100;
		int x=text().indexOf('%');
		if(x>0)
		{
			int mul=1;
			int tot=0;
			while((--x)>=0)
			{
				if(Character.isDigit(text().charAt(x)))
					tot+=CMath.s_int(""+text().charAt(x))*mul;
				else
					x=-1;
				mul=mul*10;
			}
			proff=tot;
		}
		for(int v=0;v<allAbles.size();v++)
		{
			final Ability A=allAbles.get(v).first;
			if(newMOB.fetchAbility(A.ID())==null)
			{
				final String t=A.text();
				if(t.length()>0)
				{
					x=t.indexOf('/');
					if(x<0)
						A.setMiscText("");
					else
						A.setMiscText(t.substring(x+1));
				}
				final Ability A2=newMOB.fetchEffect(A.ID());
				A.setProficiency(proff);
				newMOB.addAbility(A);
				A.setSavable(false);
				A.autoInvocation(newMOB, false);
				if(!clearedYet)
				{
					lastMOBeffects.clear();
					clearedYet=true;
				}
				if((A2==null)
				&&(!lastMOBeffects.contains(A.ID())))
					lastMOBeffects.add(A.ID());
			}
		}
		lastMOB=newMOB;
		return true;
	}

	@Override
	public void removeMyAffectsFrom(final Physical P)
	{
		if(!(P instanceof MOB))
			return;
		final PairList<Ability, Integer> V=getMySpellsV();
		final Set<String> removedAbles = new HashSet<String>();
		for(int v=0;v<V.size();v++)
		{
			final Ability A=V.get(v).first;
			if((!A.isSavable())
			&&(((MOB)P).isMine(A)))
			{
				removedAbles.add(A.ID());
				((MOB)P).delAbility(A);
			}
		}
		if(P==lastMOB)
		{
			if(removedAbles.size()>0)
			{
				final Physical lastMOB=this.lastMOB;
				final List<String> lastMOBeffects = new XVector<String>(this.lastMOBeffects);
				for(final Iterator<String> e=lastMOBeffects.iterator();e.hasNext();)
				{
					final String AID=e.next();
					final Ability A2=lastMOB.fetchEffect(AID);
					if((A2!=null)&&(removedAbles.contains(A2.ID())))
					{
						A2.unInvoke();
						lastMOB.delEffect(A2);
					}
				}
			}
			lastMOBeffects.clear();
		}
	}

	public void removeMyAffectsFromLastMob()
	{
		if(!(lastMOB instanceof MOB))
			return;
		removeMyAffectsFrom(lastMOB);
		lastMOB=null;
	}

	@Override
	public void executeMsg(final Environmental host, final CMMsg msg)
	{
	}

	@Override
	public void affectPhyStats(final Physical host, final PhyStats affectableStats)
	{
		if(processing)
			return;
		try
		{
			processing=true;
			if(host instanceof Item)
			{
				myItem=(Item)host;
			}
			else
			if(affected instanceof Item)
				myItem=(Item)affected;

			final Item I=myItem;
			if(I!=null)
			{
				if((lastMOB instanceof MOB)
				&&((I.owner()!=lastMOB)||(I.amDestroyed()))
				&&(((MOB)lastMOB).location()!=null))
					removeMyAffectsFromLastMob();

				if((lastMOB==null)
				&&(I.owner()!=null)
				&&(I.owner() instanceof MOB)
				&&(((MOB)I.owner()).location()!=null))
					addMeIfNeccessary(I.owner(),I.owner(),maxTicks);
			}
		}
		finally
		{
			processing=false;
		}
	}

	@Override
	public String getStat(final String code)
	{
		if(code == null)
			return "";
		if(code.equalsIgnoreCase("STAT-LEVEL"))
		{
			int level = 0;
			for(final Pair<Ability,Integer> p : this.getMySpellsV())
			{
				final Ability A=p.first;
				if(A!=null)
				{
					final int mul=3;
					level += (mul*CMLib.ableMapper().lowestQualifyingLevel(A.ID()));
				}
			}
			return ""+level;
		}
		else
		if(code.toUpperCase().startsWith("STAT-"))
			return "0";
		return super.getStat(code);
	}

	@Override
	public void setStat(final String code, final String val)
	{
		if(code!=null)
		{
			if(code.equalsIgnoreCase("STAT-LEVEL"))
			{

			}
			else
			if(code.equalsIgnoreCase("TONEDOWN"))
			{
				setStat("TONEDOWN-MISC",val);
			}
			else
			if((code.equalsIgnoreCase("TONEDOWN-ARMOR"))
			||(code.equalsIgnoreCase("TONEDOWN-WEAPON"))
			||(code.equalsIgnoreCase("TONEDOWN-MISC")))
			{
				/*
				final double pct=CMath.s_pct(val);
				final String s=text();
				int plusminus=s.indexOf('+');
				int minus=s.indexOf('-');
				if((minus>=0)&&((plusminus<0)||(minus<plusminus)))
					plusminus=minus;
				while(plusminus>=0)
				{
					minus=s.indexOf('-',plusminus+1);
					plusminus=s.indexOf('+',plusminus+1);
					if((minus>=0)&&((plusminus<0)||(minus<plusminus)))
						plusminus=minus;
				}
				setMiscText(s);
				*/
			}
		}
		else
			super.setStat(code, val);
	}
}
