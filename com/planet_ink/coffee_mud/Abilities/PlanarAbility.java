package com.planet_ink.coffee_mud.Abilities;
import com.planet_ink.coffee_mud.core.interfaces.*;
import com.planet_ink.coffee_mud.core.*;
import com.planet_ink.coffee_mud.core.collections.*;
import com.planet_ink.coffee_mud.core.exceptions.CMException;
import com.planet_ink.coffee_mud.core.exceptions.CoffeeMudException;
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

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/*
   Copyright 2016-2019 Bo Zimmerman

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
public class PlanarAbility extends StdAbility
{
	@Override
	public String ID()
	{
		return "PlanarAbility";
	}

	private final static String	localizedName	= CMLib.lang().L("Planar Shifting Ability");

	@Override
	public String name()
	{
		return localizedName;
	}

	@Override
	protected int canTargetCode()
	{
		return 0;
	}

	@Override
	public long flags()
	{
		return 0;
	}

	@Override
	protected int overrideMana()
	{
		return Ability.COST_ALL - 90;
	}

	@Override
	public int abstractQuality()
	{
		return Ability.QUALITY_INDIFFERENT;
	}

	protected volatile long				lastCasting		= 0;
	protected WeakReference<Room>		oldRoom			= null;
	protected Area						planeArea		= null;
	protected Map<String, String>		planeVars		= null;
	protected WeakArrayList<Room>		roomsDone		= new WeakArrayList<Room>();
	protected int						planarLevel		= 1;
	protected String					planarPrefix	= null;
	protected List<Pair<String, String>>behavList		= null;
	protected List<Pair<String, String>>reffectList		= null;
	protected List<Pair<String, String>>factionList		= null;
	protected int						bonusDmgStat	= -1;
	protected Set<String>				reqWeapons		= null;
	protected int						recoverRate		= 0;
	protected int						fatigueRate		= 0;
	protected volatile int				recoverTick		= 0;
	protected Set<PlanarSpecFlag>		specFlags		= null;
	protected int						hardBumpLevel	= 0;
	protected final Map<String,long[]>	recentVisits	= new TreeMap<String,long[]>();

	protected final static long			hardBumpTimeout	= (60L * 60L * 1000L);

	protected Pair<Pair<Integer,Integer>,List<Pair<String,String>>> enableList=null;

	public static enum PlanarVar
	{
		ID,
		TRANSITIONAL,
		ALIGNMENT,
		PREFIX,
		LEVELADJ,
		MOBRESIST,
		SETSTAT,
		BEHAVAFFID,
		ADJSTAT,
		ADJSIZE,
		ADJUST,
		MOBCOPY,
		BEHAVE,
		ENABLE,
		WEAPONMAXRANGE,
		BONUSDAMAGESTAT,
		REQWEAPONS,
		ATMOSPHERE,
		AREABLURBS,
		ABSORB,
		HOURS,
		RECOVERRATE,
		FATIGUERATE,
		REFFECT,
		AEFFECT,
		SPECFLAGS,
		MIXRACE,
		ELITE,
		ROOMCOLOR,
		ROOMADJS,
		AREAEMOTER,
		FACTIONS
	}

	public static enum PlanarSpecFlag
	{
		NOINFRAVISION,
		BADMUNDANEARMOR,
		ALLBREATHE
	}

	protected static final AtomicInteger planeIDNum = new AtomicInteger(0);

	public void clearVars()
	{
		planeArea = null;
		planarLevel=1;
		roomsDone=new WeakArrayList<Room>();
		planeVars=null;
		planarPrefix=null;
		this.behavList=null;
		this.enableList=null;
		this.reffectList=null;
		this.factionList=null;
		bonusDmgStat=-1;
		this.reqWeapons=null;
		this.recoverTick=-1;
		recoverRate		= 0;
		fatigueRate		= 0;
	}

	@Override
	public void setMiscText(final String newText)
	{
		super.setMiscText(newText);
		clearVars();
		if(newText.length()>0)
		{
			this.planeVars=getPlane(newText);
			if(this.planeVars==null)
				throw new IllegalArgumentException("Unknown: "+newText);
			this.roomsDone=new WeakArrayList<Room>();
			this.planarPrefix=planeVars.get(PlanarVar.PREFIX.toString());
			this.recoverRate = CMath.s_int(planeVars.get(PlanarVar.RECOVERRATE.toString()));
			this.fatigueRate = CMath.s_int(planeVars.get(PlanarVar.FATIGUERATE.toString()));
			this.recoverTick=1;
			if((planarPrefix!=null)&&(planarPrefix.indexOf(',')>0))
			{
				final List<String> choices=CMParms.parseCommas(planarPrefix, true);
				planarPrefix=choices.get(CMLib.dice().roll(1, choices.size(), -1));
			}
			if(affected instanceof Area)
			{
				planeArea=(Area)affected;
				int medianLevel=planeArea.getPlayerLevel();
				if(medianLevel == 0)
					medianLevel=planeArea.getAreaIStats()[Area.Stats.MED_LEVEL.ordinal()];
				planarLevel=medianLevel;
			}
			final String specflags = planeVars.get(PlanarVar.SPECFLAGS.toString());
			if(specflags != null)
			{
				for(final String s : CMParms.parse(specflags))
				{
					final PlanarSpecFlag flag=(PlanarSpecFlag)CMath.s_valueOf(PlanarSpecFlag.class, s);
					if(flag == null)
						Log.errOut("Spell_Planeshift","Unknown spec flag "+s);
					else
					{
						if(this.specFlags==null)
							this.specFlags=new HashSet<PlanarSpecFlag>();
						this.specFlags.add(flag);
					}
				}
			}
			final String areablurbs = planeVars.get(PlanarVar.AREABLURBS.toString());
			if((areablurbs!=null)&&(areablurbs.length()>0))
			{
				final Map<String,String> blurbSets=CMParms.parseEQParms(areablurbs);
				for(final String key : blurbSets.keySet())
					planeArea.addBlurbFlag(key.toUpperCase().trim().replace(' ', '_')+" "+blurbSets.get(key));
			}
			final String behaves = planeVars.get(PlanarVar.BEHAVE.toString());
			if(behaves!=null)
				this.behavList=CMParms.parseSpaceParenList(behaves);
			final String reffects = planeVars.get(PlanarVar.REFFECT.toString());
			if(reffects!=null)
				this.reffectList=CMParms.parseSpaceParenList(reffects);
			final String factions = planeVars.get(PlanarVar.FACTIONS.toString());
			if(factions!=null)
				this.factionList=CMParms.parseSpaceParenList(factions);
			final String enables = planeVars.get(PlanarVar.ENABLE.toString());
			if(enables!=null)
			{
				final List<Pair<String,String>> enableAs=CMParms.parseSpaceParenList(enables);
				Integer perLevel=Integer.valueOf(CMProps.getIntVar(CMProps.Int.LASTPLAYERLEVEL));
				Integer numSkills=Integer.valueOf(Integer.MAX_VALUE);
				for(final Iterator<Pair<String,String>> p=enableAs.iterator();p.hasNext();)
				{
					final Pair<String,String> P=p.next();
					if(P.first.toLowerCase().equals("number"))
					{
						p.remove();
						final String parms=P.second;
						final int x=parms.indexOf('/');
						if(x<0)
							numSkills=Integer.valueOf(CMath.s_int(parms.trim()));
						else
						{
							numSkills=Integer.valueOf(CMath.s_int(parms.substring(0,x).trim()));
							perLevel=Integer.valueOf(CMath.s_int(parms.substring(x+1).trim()));
						}
					}
				}
				if(enableAs.size()>0)
				{
					final PairList<String,String> addThese = new PairVector<String,String>();
					for(final Iterator<Pair<String,String>> p = enableAs.iterator();p.hasNext();)
					{
						final Pair<String,String> P=p.next();
						Ability A=CMClass.getAbility(P.first);
						if(A==null)
						{
							p.remove();
							boolean foundSomething=false;
							long flag=CMParms.indexOf(Ability.FLAG_DESCS, P.first);
							if(flag >=0 )
								flag=CMath.pow(2, flag);
							int domain=CMParms.indexOf(Ability.DOMAIN_DESCS, P.first);
							if(domain > 0)
								domain = domain << 5;
							final int acode=CMParms.indexOf(Ability.ACODE_DESCS, P.first);
							for(final Enumeration<Ability> a=CMClass.abilities();a.hasMoreElements();)
							{
								A=a.nextElement();
								if((A.Name().toUpperCase().equals(P.first))
								||((flag>0)&&(CMath.bset(A.flags(),flag)))
								||((domain>0)&&(A.classificationCode()&Ability.ALL_DOMAINS)==domain)
								||((acode>=0)&&(A.classificationCode()&Ability.ALL_ACODES)==acode)
								)
								{
									if(!addThese.containsFirst(A.ID().toUpperCase()))
									{
										addThese.add(A.ID().toUpperCase(), P.second);
										foundSomething=true;
									}
								}
							}
							if(!foundSomething)
								Log.errOut("Spell_Planeshift","Unknown skill type/domain/flag: "+P.first);
						}
					}
					enableAs.addAll(addThese);
					if(enableAs.size()>0)
						this.enableList=new Pair<Pair<Integer,Integer>,List<Pair<String,String>>>(new Pair<Integer,Integer>(numSkills,perLevel),enableAs);
				}
			}
			final String bonusDamageStat = planeVars.get(PlanarVar.BONUSDAMAGESTAT.toString());
			if(bonusDamageStat!=null)
			{
				this.bonusDmgStat=CMParms.indexOf(CharStats.CODES.BASENAMES(), bonusDamageStat.toUpperCase().trim());
			}
			final String reqWeapons = planeVars.get(PlanarVar.REQWEAPONS.toString());
			if(reqWeapons != null)
				this.reqWeapons = new HashSet<String>(CMParms.parse(reqWeapons.toUpperCase().trim()));
			final String atmosphere = planeVars.get(PlanarVar.ATMOSPHERE.toString());
			if(atmosphere!=null)
			{
				if(atmosphere.length()==0)
					this.planeArea.setAtmosphere(Integer.MIN_VALUE);
				else
				{
					final int atmo=RawMaterial.CODES.FIND_IgnoreCase(atmosphere);
					this.planeArea.setAtmosphere(atmo);
				}
			}
			final String absorb = planeVars.get(PlanarVar.ABSORB.toString());
			if(absorb != null)
				reEffect(planeArea,"Prop_AbsorbDamage",absorb);
			final TimeClock C=(TimeClock)CMLib.time().globalClock().copyOf();
			C.setDayOfMonth(1);
			C.setYear(1);
			C.setMonth(1);
			C.setHourOfDay(0);
			final String hours = planeVars.get(PlanarVar.HOURS.toString());
			if((hours != null)&&(CMath.isInteger(hours)))
			{
				final double mul=CMath.div(CMath.s_int(hours),CMLib.time().globalClock().getHoursInDay());
				if(mul != 1.0)
				{
					final int newHours = (int)Math.round(CMath.mul(C.getHoursInDay(),mul));
					C.setHoursInDay(newHours);
					C.setDawnToDusk((int)Math.round(CMath.mul(C.getDawnToDusk()[0],mul))
									, (int)Math.round(CMath.mul(C.getDawnToDusk()[1],mul))
									, (int)Math.round(CMath.mul(C.getDawnToDusk()[2],mul))
									, (int)Math.round(CMath.mul(C.getDawnToDusk()[3],mul)));
				}
			}
			planeArea.setTimeObj(C);
			planeArea.addNonUninvokableEffect(CMClass.getAbility("Prop_NoTeleportOut"));
			planeArea.addNonUninvokableEffect(CMClass.getAbility("Prop_NoTeleport"));
			planeArea.addNonUninvokableEffect(CMClass.getAbility("Prop_NoRecall"));
			final String aeffects = planeVars.get(PlanarVar.AEFFECT.toString());
			if(aeffects!=null)
			{
				final List<Pair<String,String>> affectList=CMParms.parseSpaceParenList(aeffects);
				if(affectList!=null)
				{
					for(final Pair<String,String> p : affectList)
					{
						if(planeArea.fetchBehavior(p.first)==null)
						{
							final Behavior B=CMClass.getBehavior(p.first);
							if(B==null)
							{
								final Ability A=CMClass.getAbility(p.first);
								if(A==null)
									Log.errOut("Spell_Planeshift","Unknown behavior : "+p.first);
								else
								{
									A.setMiscText(p.second);
									planeArea.addNonUninvokableEffect(A);
								}
							}
							else
							{
								B.setParms(p.second);
								planeArea.addBehavior(B);
							}
						}
					}
				}
			}
		}
	}

	protected void reEffect(final Physical M, final String ID, final String parms)
	{
		if(M!=null)
		{
			Ability A=M.fetchEffect(ID);
			if(A!=null)
				M.delEffect(A);
			else
				A=CMClass.getAbility(ID);
			if(A!=null)
			{
				M.addNonUninvokableEffect(A);
				A.setMiscText((parms+" "+A.text()).trim());
			}
		}
	}

	public synchronized void fixRoom(final Room room)
	{
		try
		{
			room.toggleMobility(false);
			CMLib.threads().suspendResumeRecurse(room, false, true);
			for(int i=0;i<Directions.NUM_DIRECTIONS();i++)
			{
				final Room R=room.rawDoors()[i];
				if((R!=null)&&(R.getArea()!=planeArea))
					room.rawDoors()[i]=null;
			}
			if(planeVars.containsKey(PlanarVar.ATMOSPHERE.toString()))
				room.setAtmosphere(planeArea.getAtmosphere());
			int eliteLevel=0;
			if(planeVars.containsKey(PlanarVar.ELITE.toString()))
				eliteLevel=CMath.s_int(planeVars.get(PlanarVar.ELITE.toString()));
			if(planeVars.containsKey(PlanarVar.ROOMCOLOR.toString()))
			{
				String prefix="";
				String displayText = room.displayText();
				if(displayText.toUpperCase().startsWith("<VARIES>"))
				{
					prefix="<VARIES>";
					displayText=displayText.substring(prefix.length());
				}
				String color=planeVars.get(PlanarVar.ROOMCOLOR.toString());
				if(color.startsWith("UP "))
				{
					color=color.substring(3).trim();
					displayText=displayText.toUpperCase();
				}
				room.setDisplayText(prefix+color+displayText+"^N");
			}
			if(planeVars.containsKey(PlanarVar.ROOMADJS.toString()))
			{
				String wordStr=planeVars.get(PlanarVar.ROOMADJS.toString());
				String prefix="";
				String desc = room.description();
				if(wordStr.startsWith("UP "))
				{
					wordStr=wordStr.substring(3).trim();
					desc=desc.toUpperCase();
				}
				int chance=30;
				final int x=wordStr.indexOf(' ');
				if((x>0)&&(CMath.isInteger(wordStr.substring(0, x))))
				{
					chance=CMath.s_int(wordStr.substring(0, x));
					wordStr=wordStr.substring(x+1).trim();
				}
				final String[] words= wordStr.split(",");
				if(desc.toUpperCase().startsWith("<VARIES>"))
				{
					prefix="<VARIES>";
					desc=desc.substring(prefix.length());
				}
				room.setDescription(prefix+CMLib.english().insertAdjectives(desc, words, chance));
			}
			if(this.reffectList!=null)
			{
				for(final Pair<String,String> p : this.reffectList)
				{
					if(room.fetchBehavior(p.first)==null)
					{
						final Behavior B=CMClass.getBehavior(p.first);
						if(B==null)
						{
							final Ability A=CMClass.getAbility(p.first);
							if(A==null)
								Log.errOut("Spell_Planeshift","Unknown behavior : "+p.first);
							else
							{
								A.setMiscText(p.second);
								room.addNonUninvokableEffect(A);
							}
						}
						else
						{
							B.setParms(p.second);
							room.addBehavior(B);
						}
					}
				}
			}

			if(CMLib.law().getLandTitle(room)!=null)
			{
				final List<Physical> destroyMe=new ArrayList<Physical>();
				final Set<Rider> protSet = new HashSet<Rider>();
				for(final Enumeration<MOB> m=room.inhabitants();m.hasMoreElements();)
				{
					final MOB M=m.nextElement();
					if((M!=null)&&(M.isPlayer()))
					{
						protSet.add(M);
						M.getGroupMembersAndRideables(protSet);
					}
				}
				for(final Enumeration<MOB> m=room.inhabitants();m.hasMoreElements();)
				{
					final MOB M=m.nextElement();
					if((M!=null) && (!M.isPlayer()) && (!protSet.contains(M)))
						destroyMe.add(M);
				}
				for(final Enumeration<Item> i=room.items();i.hasMoreElements();)
				{
					final Item I=i.nextElement();
					if((I!=null)&&(!protSet.contains(I)))
						destroyMe.add(I);
				}
				for(final Physical P : destroyMe)
					P.destroy();
			}
			final int allLevelAdj=CMath.s_int(planeVars.get(PlanarVar.LEVELADJ.toString()));
			final List<Item> delItems=new ArrayList<Item>(0);
			for(final Enumeration<Item> i=room.items();i.hasMoreElements();)
			{
				final Item I=i.nextElement();
				if(I==null)
					continue;
				if((I instanceof Exit)&&((I instanceof BoardableShip)))
				{
					for(int x=0;x<100;x++)
					{
						final Room R2=((Exit)I).lastRoomUsedFrom(room);
						if((R2!=null)&&(R2.getArea()!=planeArea))
						{
							delItems.add(I);
							break;
						}
					}
				}
				else
				if(I instanceof Exit)
					I.setReadableText("");
				else
				if((invoker!=null)&&((I instanceof Weapon)||(I instanceof Armor)))
				{
					final int newILevelAdj = (planarLevel - I.phyStats().level());
					int newILevel=invoker.phyStats().level() - newILevelAdj + allLevelAdj;
					if(newILevel <= 0)
						newILevel = 1;
					CMLib.itemBuilder().itemFix(I, newILevel, null);
					I.basePhyStats().setLevel(newILevel);
					I.phyStats().setLevel(newILevel);
					CMLib.itemBuilder().balanceItemByLevel(I);
					if((I instanceof Weapon)
					&&(this.reqWeapons!=null)
					&&(this.reqWeapons.contains("MAGICAL")))
					{
						I.basePhyStats().setDisposition(I.basePhyStats().disposition()|PhyStats.IS_BONUS);
						I.phyStats().setDisposition(I.phyStats().disposition()|PhyStats.IS_BONUS);
					}
					I.text();
				}
			}
			for(final Item I : delItems)
				I.destroy();
			for(final Enumeration<MOB> m=room.inhabitants();m.hasMoreElements();)
			{
				final MOB M=m.nextElement();
				if((M!=null)
				&&(invoker!=null)
				&&(M.isMonster())
				&&(M.getStartRoom()!=null)
				&&(M.getStartRoom().getArea()==planeArea))
				{
					if(planeVars.containsKey(PlanarVar.MIXRACE.toString()))
					{
						final String mixRace = planeVars.get(PlanarVar.MIXRACE.toString());
						final Race firstR=CMClass.getRace(mixRace);
						if(firstR==null)
							Log.errOut("PlanarAbility","Unknown mixrace: "+mixRace);
						else
						{
							final Race secondR=M.charStats().getMyRace();
							final Race R=CMLib.utensils().getMixedRace(firstR.ID(),secondR.ID(), false);
							if(R!=null)
							{
								M.baseCharStats().setMyRace(R);
								M.charStats().setMyRace(R);
								M.charStats().setWearableRestrictionsBitmap(M.charStats().getWearableRestrictionsBitmap()|M.charStats().getMyRace().forbiddenWornBits());
							}
						}
					}

					if(planeVars.containsKey(PlanarVar.ATMOSPHERE.toString()))
						M.baseCharStats().setBreathables(new int[]{room.getAtmosphere()});
					final int newLevelAdj = (planarLevel - M.phyStats().level());
					int newLevel = invoker.phyStats().level() - newLevelAdj + allLevelAdj;
					if(newLevel <= 0)
						newLevel = 1;
					if((planarPrefix!=null)&&(planarPrefix.length()>0))
					{
						final String oldName=M.Name();
						int x;
						if(oldName.toLowerCase().indexOf(planarPrefix.toLowerCase())<0)
						{
							if(CMLib.english().startsWithAnArticle(M.Name()))
							{
								final String Name = M.Name().substring(M.Name().indexOf(' ')).trim();
								M.setName(CMLib.english().startWithAorAn(planarPrefix+" "+Name));
							}
							else
							{
								M.setName(CMStrings.capitalizeFirstLetter(planarPrefix)+" "+M.Name());
							}
							if((x=M.displayText().toLowerCase().indexOf(oldName.toLowerCase()))>=0)
							{
								M.setDisplayText(M.displayText().substring(0,x)+M.Name()+M.displayText().substring(x+oldName.length()));
							}
							else
							if(CMLib.english().startsWithAnArticle(M.displayText()))
							{
								final String Name = M.displayText().substring(M.displayText().indexOf(' ')).trim();
								M.setDisplayText(CMLib.english().startWithAorAn(planarPrefix+" "+Name));
							}
							else
							if((x=M.displayText().toLowerCase().indexOf(M.charStats().getMyRace().name().toLowerCase()))>=0)
							{
								final int len=M.charStats().getMyRace().name().toLowerCase().length();
								M.setDisplayText(M.displayText().substring(0,x)+planarPrefix+M.Name()+M.displayText().substring(x+len));
							}
						}
					}
					M.basePhyStats().setLevel(newLevel);
					M.phyStats().setLevel(newLevel);
					CMLib.leveler().fillOutMOB(M,M.basePhyStats().level()+hardBumpLevel);
					M.basePhyStats().setLevel(newLevel);
					M.phyStats().setLevel(newLevel);
					final String align=planeVars.get(PlanarVar.ALIGNMENT.toString());
					if(align!=null)
					{
						M.removeFaction(CMLib.factions().getAlignmentID());
						M.addFaction(CMLib.factions().getAlignmentID(), CMath.s_int(align));
					}
					if(this.factionList!=null)
					{
						for(final Pair<String,String> p : this.factionList)
						{
							Faction F=null;
							if(CMLib.factions().isFactionID(p.first))
								F=CMLib.factions().getFaction(p.first);
							if(F==null)
								F=CMLib.factions().getFactionByName(p.first);
							if(F!=null)
							{
								if(CMath.isInteger(p.second))
								{
									M.removeFaction(F.factionID());
									M.addFaction(F.factionID(), CMath.s_int(p.second));
								}
								else
								{
									final Faction.FRange FR = F.fetchRange(p.second);
									if(FR != null)
									{
										M.removeFaction(F.factionID());
										M.addFaction(F.factionID(), FR.random());
									}
								}
							}
						}
					}
					for(final Enumeration<Item> mi=M.items();mi.hasMoreElements();)
					{
						final Item mI=mi.nextElement();
						if((mI!=null)&&(invoker!=null))
						{
							final int newILevelAdj = (planarLevel - mI.phyStats().level());
							int newILevel=invoker.phyStats().level() + newILevelAdj + allLevelAdj;
							if(newILevel <= 0)
								newILevel = 1;
							mI.basePhyStats().setLevel(newILevel);
							mI.phyStats().setLevel(newILevel);
							CMLib.itemBuilder().balanceItemByLevel(mI);
							if((mI instanceof Weapon)
							&&(this.reqWeapons!=null)
							&&(this.reqWeapons.contains("MAGICAL")))
							{
								mI.basePhyStats().setDisposition(mI.basePhyStats().disposition()|PhyStats.IS_BONUS);
								mI.phyStats().setDisposition(mI.phyStats().disposition()|PhyStats.IS_BONUS);
							}
							mI.text();
						}
					}
					final String resistWeak = planeVars.get(PlanarVar.MOBRESIST.toString());
					if(resistWeak != null)
						reEffect(M,"Prop_Resistance",resistWeak);
					else
					if(this.hardBumpLevel>0)
						reEffect(M,"Prop_Resistance","magic holy disease poison evil weapons "+(5*hardBumpLevel)+"% ");
					final String setStat = planeVars.get(PlanarVar.SETSTAT.toString());
					if(setStat != null)
						reEffect(M,"Prop_StatTrainer",setStat);
					final String behavaffid=planeVars.get(PlanarVar.BEHAVAFFID.toString());
					if(behavaffid!=null)
					{
						String changeToID;
						for(final Enumeration<Behavior> b=M.behaviors();b.hasMoreElements();)
						{
							final Behavior B=b.nextElement();
							if((B!=null)&&((changeToID=CMParms.getParmStr(behavaffid, B.ID(), "")).length()>0))
							{
								boolean copyParms=false;
								if(changeToID.startsWith("*"))
								{
									copyParms=true;
									changeToID=changeToID.substring(1);
								}
								M.delBehavior(B);
								final Behavior B2=CMClass.getBehavior(changeToID);
								if(B2 != null)
								{
									if(copyParms)
										B2.setParms(B.getParms());
									M.addBehavior(B2);
								}
							}
						}
					}
					final String adjStat = planeVars.get(PlanarVar.ADJSTAT.toString());
					if(adjStat != null)
						reEffect(M,"Prop_StatAdjuster",adjStat);
					if(eliteLevel > 0)
					{
						switch(eliteLevel)
						{
						case 1:
							reEffect(M,"Prop_Adjuster", "multiplych=true hitpoints+300 multiplyph=true attack+150 damage+150 armor+115 ALLSAVES+15");
							reEffect(M,"Prop_ShortEffects", "");
							break;
						default:
							reEffect(M,"Prop_Adjuster", "multiplych=true hitpoints+600 multiplyph=true attack+150 damage+150 armor+115 ALLSAVES+15");
							reEffect(M,"Prop_ShortEffects", "");
							break;
						}
						reEffect(M,"Prop_ModExperience","*2");
						final String adjSize = planeVars.get(PlanarVar.ADJSIZE.toString());
						if(adjSize != null)
						{
							final double heightAdj = CMParms.getParmDouble(adjSize, "HEIGHT", Double.MIN_VALUE);
							if(heightAdj > Double.MIN_VALUE)
								reEffect(M,"Prop_Adjuster","height+"+(100+(heightAdj*100)));
						}
					}
					else
					{
						final String adjust = planeVars.get(PlanarVar.ADJUST.toString());
						if(adjust != null)
							reEffect(M,"Prop_Adjuster",adjust);
						final String adjSize = planeVars.get(PlanarVar.ADJSIZE.toString());
						if(adjSize != null)
						{
							final double heightAdj = CMParms.getParmDouble(adjSize, "HEIGHT", Double.MIN_VALUE);
							if(heightAdj > Double.MIN_VALUE)
								reEffect(M,"Prop_Adjuster","height+"+(int)Math.round(CMath.mul(M.basePhyStats().height(),heightAdj)));
						}
					}
					final String adjSize = planeVars.get(PlanarVar.ADJSIZE.toString());
					if(adjSize != null)
					{
						final double weightAdj = CMParms.getParmDouble(adjSize, "WEIGHT", Double.MIN_VALUE);
						if(weightAdj > Double.MIN_VALUE)
							reEffect(M,"Prop_StatAdjuster","weightadj="+(int)Math.round(CMath.mul(M.baseWeight(),weightAdj)));
					}
					if(this.behavList!=null)
					{
						for(final Pair<String,String> p : this.behavList)
						{
							if(M.fetchBehavior(p.first)==null)
							{
								final Behavior B=CMClass.getBehavior(p.first);
								if(B==null)
									Log.errOut("Spell_Planeshift","Unknown behavior : "+p.first);
								else
								{
									B.setParms(p.second);
									M.addBehavior(B);
								}
							}
						}
					}
					if(this.enableList != null)
					{
						final Pair<Integer,Integer> lv=this.enableList.first;
						final PairList<String,String> unused = new PairVector<String,String>(this.enableList.second);
						for(int l=0;l<M.phyStats().level() && (unused.size()>0);l+=lv.second.intValue())
						{
							for(int a=0;a<lv.first.intValue()  && (unused.size()>0);a++)
							{
								final int aindex=CMLib.dice().roll(1, unused.size(), -1);
								final Pair<String,String> P=unused.remove(aindex);
								final Ability A=CMClass.getAbility(P.first);
								if(M.fetchAbility(A.ID())==null)
								{
									A.setMiscText(P.second);
									M.addAbility(A);
								}
							}
						}
					}
					M.text();
					M.recoverCharStats();
					M.recoverMaxState();
					M.recoverPhyStats();
					M.recoverCharStats();
					M.recoverMaxState();
					M.recoverPhyStats();
					M.resetToMaxState();
				}
			}
			final int mobCopy=CMath.s_int(planeVars.get(PlanarVar.MOBCOPY.toString()));
			if(mobCopy>0)
			{
				final List<MOB> list=new ArrayList<MOB>(room.numInhabitants());
				for(final Enumeration<MOB> m=room.inhabitants();m.hasMoreElements();)
				{
					final MOB M=m.nextElement();
					if((M!=null)
					&&(M.isMonster())
					&&(M.getStartRoom()!=null)
					&&(M.getStartRoom().getArea()==planeArea))
					{
						list.add(M);
					}
				}
				for(final MOB M : list)
				{
					for(int i=0;i<mobCopy;i++)
					{
						final MOB M2=(MOB)M.copyOf();
						M2.text();
						M2.setSavable(M.isSavable());
						M2.bringToLife(room, true);
						M2.recoverCharStats();
						M2.recoverMaxState();
						M2.recoverPhyStats();
					}
				}
			}
		}
		catch(final Exception e)
		{
			Log.errOut(e);
		}
		finally
		{
			room.recoverRoomStats();
			CMLib.threads().suspendResumeRecurse(room, false, false);
			room.toggleMobility(true);
			room.recoverRoomStats();
		}
	}

	@Override
	public void affectCharStats(final MOB affected, final CharStats affectableStats)
	{
		super.affectCharStats(affected, affectableStats);
		if(this.specFlags!=null)
		{
			if(this.specFlags.contains(PlanarSpecFlag.ALLBREATHE))
				affectableStats.setBreathables(new int[]{});
		}
	}

	@Override
	public void affectPhyStats(final Physical affected, final PhyStats affectableStats)
	{
		super.affectPhyStats(affected, affectableStats);
		if(affected instanceof MOB)
		{
			if(this.bonusDmgStat>=0)
				affectableStats.setDamage(affectableStats.damage() + (((((MOB)affected).charStats().getStat(this.bonusDmgStat))-10)/2));
			if(this.specFlags!=null)
			{
				if(this.specFlags.contains(PlanarSpecFlag.NOINFRAVISION))
					affectableStats.setSensesMask(CMath.unsetb(affectableStats.sensesMask(), PhyStats.CAN_SEE_INFRARED));
				if(this.specFlags.contains(PlanarSpecFlag.BADMUNDANEARMOR))
				{
					final MOB M=(MOB)affected;
					int neg=0;
					for(final Enumeration<Item> i=M.items();i.hasMoreElements();)
					{
						final Item I=i.nextElement();
						if((I instanceof Armor)
						&&(!I.amWearingAt(Wearable.IN_INVENTORY))
						&&((!I.amWearingAt(Wearable.WORN_FLOATING_NEARBY))||(I.fitsOn(Wearable.WORN_FLOATING_NEARBY)))
						&&((!I.amWearingAt(Wearable.WORN_HELD))||(this instanceof Shield))
						&&(!CMLib.flags().isABonusItems(I))
						&&(I.phyStats().ability()<=0))
						{
							neg += I.phyStats().armor();
						}
					}
					affectableStats.setArmor(affectableStats.armor()+neg);
				}
			}
		}
	}

	@Override
	public boolean tick(final Tickable ticking, final int tickID)
	{
		if(!super.tick(ticking, tickID))
			return false;
		if((ticking instanceof Area)&&(tickID == Tickable.TICKID_AREA))
		{
			if(((this.recoverRate>0)||(this.fatigueRate>0)) &&(--this.recoverTick <= 0) && (this.planeArea!=null))
			{
				this.recoverTick = CMProps.getIntVar(CMProps.Int.RECOVERRATE) * CharState.REAL_TICK_ADJUST_FACTOR;
				for(final Enumeration<Room> r=planeArea.getFilledProperMap();r.hasMoreElements();)
				{
					final Room R=r.nextElement();
					if((R!=null)&&(R.numPCInhabitants()>0))
					{
						for(final Enumeration<MOB> m=R.inhabitants();m.hasMoreElements();)
						{
							final MOB M=m.nextElement();
							for(int i=0;i<this.recoverRate;i++)
								CMLib.combat().recoverTick(M);
							if(this.fatigueRate>100)
							{
								M.curState().setHunger(M.maxState().maxHunger(M.baseWeight()));
								M.curState().setThirst(M.maxState().maxThirst(M.baseWeight()));
								M.curState().setFatigue(0);
							}
							else
							for(int i=0;i<(this.recoverTick * this.fatigueRate);i++)
								CMLib.combat().expendEnergy(M, false);
						}
					}
				}
			}
		}
		return true;
	}

	protected boolean roomDone(final Room R)
	{
		synchronized(roomsDone)
		{
			return (this.roomsDone.contains(R));
		}
	}

	protected synchronized void doneRoom(final Room R)
	{
		synchronized(roomsDone)
		{
			this.roomsDone.add(R);
		}
	}

	@Override
	public void executeMsg(final Environmental myHost, final CMMsg msg)
	{
		if(msg.targetMinor()==CMMsg.TYP_NEWROOM)
		{
			if((msg.target() instanceof Room)
			&&(!roomDone((Room)msg.target()))
			&&(((Room)msg.target()).getArea()==planeArea))
			{
				doneRoom((Room)msg.target());
				fixRoom((Room)msg.target());
			}
		}
		super.executeMsg(myHost, msg);
	}

	@Override
	public boolean okMessage(final Environmental myHost, final CMMsg msg)
	{
		if(!super.okMessage(myHost, msg))
			return false;
		switch(msg.targetMinor())
		{
		case CMMsg.TYP_WEAPONATTACK:
			if((msg.tool() instanceof AmmunitionWeapon)
			&&(((AmmunitionWeapon)msg.tool()).requiresAmmunition())
			&&(msg.target() instanceof MOB)
			&&(planeVars!=null)
			&&(planeVars.containsKey(PlanarVar.WEAPONMAXRANGE.toString()))
			&&((msg.source().rangeToTarget()>0)||(((MOB)msg.target()).rangeToTarget()>0)))
			{
				final int maxRange=CMath.s_int(planeVars.get(PlanarVar.WEAPONMAXRANGE.toString()));
				if(((msg.source().rangeToTarget()>maxRange)||(((MOB)msg.target()).rangeToTarget()>maxRange)))
				{
					final String ammo=((AmmunitionWeapon)msg.tool()).ammunitionType();
					final String msgOut=L("The @x1 fired by <S-NAME> from <O-NAME> at <T-NAME> stops moving!",ammo);
					final Room R=msg.source().location();
					if(R!=null)
						R.show(msg.source(), msg.target(), msg.tool(), CMMsg.MSG_OK_VISUAL, msgOut);
					return false;
				}
			}
			break;
		case CMMsg.TYP_DAMAGE:
			if((msg.tool() instanceof Weapon)
			&&(this.reqWeapons!=null)
			&&(msg.value()>0))
			{
				if((CMLib.flags().isABonusItems((Weapon)msg.tool()) && (this.reqWeapons.contains("MAGICAL")))
				||(this.reqWeapons.contains(Weapon.CLASS_DESCS[((Weapon)msg.tool()).weaponClassification()]))
				||(this.reqWeapons.contains(Weapon.TYPE_DESCS[((Weapon)msg.tool()).weaponDamageType()])))
				{ // pass
				}
				else
					msg.setValue(0);
			}
			break;
		}
		return true;
	}

	protected static List<String> getAllPlaneKeys()
	{
		final Map<String,Map<String,String>> map = getPlaneMap();
		final List<String> transitions=new ArrayList<String>(map.size());
		for(final String key : map.keySet())
			transitions.add(key);
		return transitions;
	}

	protected static List<String> getTransitionPlaneKeys()
	{
		final Map<String,Map<String,String>> map = getPlaneMap();
		final List<String> transitions=new ArrayList<String>(2);
		for(final String key : map.keySet())
		{
			final Map<String,String> entry=map.get(key);
			if(CMath.s_bool(entry.get(PlanarVar.TRANSITIONAL.toString())))
				transitions.add(key);
		}
		return transitions;
	}

	protected static String listOfPlanes()
	{
		final Map<String,Map<String,String>> map = getPlaneMap();
		final StringBuilder str=new StringBuilder();
		for(final String key : map.keySet())
		{
			final Map<String,String> entry=map.get(key);
			str.append(entry.get(PlanarVar.ID.toString())).append(", ");
		}
		if(str.length()<2)
			return "";
		return str.toString();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Map<String,Map<String,String>> getPlaneMap()
	{
		Map<String,Map<String,String>> map = (Map)Resources.getResource("SKILL_PLANES_OF_EXISTENCE");
		if(map == null)
		{
			map = new TreeMap<String,Map<String,String>>();
			final CMFile F=new CMFile(Resources.makeFileResourceName("skills/planesofexistence.txt"),null);
			final List<String> lines = Resources.getFileLineVector(F.text());
			for(String line : lines)
			{
				line=line.trim();
				String planename=null;
				if(line.startsWith("\""))
				{
					final int x=line.indexOf("\"",1);
					if(x>1)
					{
						planename=line.substring(1,x);
						line=line.substring(x+1).trim();
					}
				}
				if(planename != null)
				{
					final Map<String,String> planeParms = CMParms.parseEQParms(line);
					for(final String key : planeParms.keySet())
					{
						if((CMath.s_valueOf(PlanarVar.class, key)==null)
						&&(CMLib.factions().getFaction(key)==null)
						&&(CMLib.factions().getFactionByName(key)==null))
							Log.errOut("Spell_Planeshift","Unknown planar var: "+key);
					}
					planeParms.put(PlanarVar.ID.toString(), planename);
					map.put(planename.toUpperCase(), planeParms);
				}
			}
			Resources.submitResource("SKILL_PLANES_OF_EXISTENCE", map);
		}
		return map;
	}

	protected static Map<String,String> getPlane(String name)
	{
		final Map<String,Map<String,String>> map = getPlaneMap();
		name=name.trim().toUpperCase();
		if(map.containsKey(name))
			return map.get(name);
		for(final String key : map.keySet())
		{
			if(key.startsWith(name))
				return map.get(key);
		}
		for(final String key : map.keySet())
		{
			if(key.indexOf(name)>=0)
				return map.get(key);
		}
		for(final String key : map.keySet())
		{
			if(key.endsWith(name))
				return map.get(key);
		}
		return null;
	}

	protected void destroyPlane(final Area planeA)
	{
		if(planeA != null)
		{
			Area parentArea = null;
			int x=planeA.Name().indexOf('_');
			if(x<0)
				x=planeA.Name().indexOf(' ');
			if(x>=0)
				parentArea = CMLib.map().getArea(Name().substring(x+1));

			for(final Enumeration<Room> r=planeA.getFilledProperMap();r.hasMoreElements();)
			{
				final Room R=r.nextElement();
				if(R!=null)
				{
					if(R.numInhabitants()>0)
						R.showHappens(CMMsg.MSG_OK_ACTION, L("This plane is fading away..."));
					for(final Enumeration<MOB> i=R.inhabitants();i.hasMoreElements();)
					{
						final MOB M=i.nextElement();
						if((M!=null)
						&&(M.isPlayer()))
						{
							Room oldRoom = (this.oldRoom!=null) ? CMLib.map().getRoom(this.oldRoom.get()) : null;
							if((oldRoom==null)
							||(oldRoom.amDestroyed())
							||(oldRoom.getArea()==null)
							||(!oldRoom.getArea().isRoom(oldRoom)))
								oldRoom=M.getStartRoom();
							for(int i1=0; (i1<50) && (oldRoom != R) && (R.isInhabitant(M) || M.location()==R);i1++)
							{
								oldRoom.bringMobHere(M, true);
								CMLib.commands().postLook(M,true);
								R.delInhabitant(M);
							}
						}
					}
					for(final Enumeration<Item> i=R.items();i.hasMoreElements();)
					{
						final Item I=i.nextElement();
						if((I instanceof DeadBody)
						&&(((DeadBody)I).isPlayerCorpse()))
						{
							if((parentArea != null)
							&&(R.roomID().length()>0)
							&&(R.roomID().indexOf(parentArea.Name()+"#")>=0)
							&&(parentArea.getRoom(parentArea.Name()+R.roomID().substring(R.roomID().lastIndexOf('#'))))!=null)
							{
								final Room sendR=parentArea.getRoom(parentArea.Name()+R.roomID().substring(R.roomID().lastIndexOf('#')));
								sendR.moveItemTo(I);
							}
							else
							{
								MOB M=((DeadBody)I).getSavedMOB();
								if(M==null)
									M=CMLib.players().getPlayerAllHosts(((DeadBody)I).getMobName());
								if(M!=null)
								{
									if(M.location()!=null)
										M.location().moveItemTo(I);
									else
									if(M.getStartRoom()!=null)
										M.getStartRoom().moveItemTo(I);
								}
							}
						}
					}
				}
			}
			final MOB mob=CMClass.getFactoryMOB();
			try
			{
				final CMMsg msg=CMClass.getMsg(mob,null,null,CMMsg.MSG_EXPIRE,null);
				final LinkedList<Room> propRooms = new LinkedList<Room>();
				for(final Enumeration<Room> r=planeA.getFilledProperMap();r.hasMoreElements();)
					propRooms.add(r.nextElement());
				// sends everyone home
				for(final Iterator<Room> r=propRooms.iterator();r.hasNext();)
				{
					final Room R=r.next();
					try
					{
						CMLib.map().emptyRoom(R, null, true);
					}
					catch(final Exception e)
					{
						Log.errOut(e);
					}
				}
				// msgs only, handles saves and stuff, but ignores grid rooms!!
				for(final Iterator<Room> r=propRooms.iterator();r.hasNext();)
				{
					final Room R=r.next();
					try
					{
						try
						{
							R.clearSky();
							msg.setTarget(R);
							R.executeMsg(mob,msg);
						}
						catch(final Exception e)
						{
							Log.errOut(e);
						}
						R.destroy(); // destroys the mobs and items.  the Deadly Thing.
					}
					catch(final Exception e)
					{
						Log.errOut(e);
					}
				}
				propRooms.clear();
				CMLib.map().delArea(planeA);
				planeA.destroy();
			}
			finally
			{
				mob.destroy();
			}
			planeA.destroy();
		}
	}

	protected void destroyPlane()
	{
		destroyPlane(planeArea);
		this.planeArea=null;
	}

	protected String getStrippedRoomID(final String roomID)
	{
		final int x=roomID.indexOf('#');
		if(x<0)
			return null;
		return roomID.substring(x);
	}

	protected String convertToMyArea(final String Name, final String roomID)
	{
		final String strippedID=getStrippedRoomID(roomID);
		if(strippedID==null)
			return null;
		return Name+strippedID;
	}

	@Override
	public void setAffectedOne(final Physical P)
	{
		super.setAffectedOne(P);
		if(invoker != null)
		{
			final MOB mob=invoker;
			final Room R=mob.location();
			if(R!=null)
			{
				final PlanarAbility currentShift = getPlanarAbility(R.getArea());
				if((currentShift != null)&&(currentShift.oldRoom!=null)&&(currentShift.oldRoom.get()!=null))
					oldRoom=currentShift.oldRoom;
				else
				if(currentShift != null)
					oldRoom=new WeakReference<Room>(mob.getStartRoom());
				else
					oldRoom=new WeakReference<Room>(R);
			}
		}
	}

	protected PlanarAbility getPlanarAbility(final Physical P)
	{
		for(final Enumeration<Ability> a=P.effects();a.hasMoreElements();)
		{
			final Ability A=a.nextElement();
			if(A instanceof PlanarAbility)
				return (PlanarAbility)A;
		}
		return null;
	}

	protected String castingMessage(final MOB mob, final boolean auto)
	{
		return auto?L(""):L("^S<S-NAME> conjur(s) a powerful planar connection!^?");
	}

	protected String failMessage(final MOB mob, final boolean auto)
	{
		return L("^S<S-NAME> attempt(s) to conjure a powerful planar connection, and fails.");
	}

	@Override
	public void unInvoke()
	{
		if(canBeUninvoked())
		{
			destroyPlane();
		}
		super.unInvoke();
	}

	protected boolean alwaysRandomArea=false;

	@Override
	public boolean invoke(final MOB mob, final List<String> commands, final Physical givenTarget, final boolean auto, final int asLevel)
	{
		oldRoom = null;
		clearVars();

		if(commands.size()<1)
		{
			mob.tell(L("Go where?"));
			mob.tell(L("Known planes: @x1",listOfPlanes()+L("Prime Material")));
			return false;
		}
		String planeName=CMParms.combine(commands,0).trim().toUpperCase();
		int planeNameCt=0;
		if(planeName.toLowerCase().endsWith("prime material"))
			planeName="Prime Material";
		else
		while((getPlane(planeName)==null)&&(commands.size()>planeNameCt))
			planeName=CMParms.combine(commands,++planeNameCt).trim().toUpperCase();
		oldRoom=new WeakReference<Room>(mob.location());
		Area cloneArea = mob.location().getArea();
		final Area mobArea = cloneArea;
		String cloneRoomID=CMLib.map().getExtendedRoomID(mob.location());
		final PlanarAbility currentShift = getPlanarAbility(mobArea);
		if(planeName.equalsIgnoreCase("Prime Material"))
		{
			if(!super.invoke(mob,commands,givenTarget,auto,asLevel))
				return false;
			if(currentShift == null)
			{
				mob.tell(L("You are already on the prime material plane."));
				return false;
			}
			final boolean success=proficiencyCheck(mob,0,auto);
			if(success)
			{
				final CMMsg msg=CMClass.getMsg(mob,null,this,CMMsg.MASK_MOVE|verbalCastCode(mob,null,auto),castingMessage(mob, auto));
				if(mob.location().okMessage(mob,msg))
				{
					mob.location().send(mob,msg);
					currentShift.unInvoke();
				}
			}
			else
			{
				this.beneficialVisualFizzle(mob, null, failMessage(mob, auto));
			}
			return true;
		}
		else
		if(this.lastCasting > (System.currentTimeMillis() - (10 * TimeManager.MILI_MINUTE)))
		{
			final Ability A=CMClass.getAbility("Disease_PlanarInstability");
			if((A!=null)
			&&(!CMSecurity.isDisabled(CMSecurity.DisFlag.AUTODISEASE))
			&&(!CMSecurity.isAbilityDisabled(A.ID())))
				A.invoke(mob, mob, true, 0);
		}
		Map<String,String> planeFound = getPlane(planeName);
		if(planeFound == null)
		{
			mob.tell(L("There is no known plane '@x1'.",planeName));
			mob.tell(L("Known planes: @x1",listOfPlanes()+L("Prime Material")));
			return false;
		}
		planeName = planeFound.get(PlanarVar.ID.toString()).toUpperCase().trim();

		if(!super.invoke(mob,commands,givenTarget,auto,asLevel))
			return false;

		final boolean success=proficiencyCheck(mob,0,auto);
		if((currentShift!=null)&&(currentShift.text().equalsIgnoreCase(planeName)))
		{
			this.beneficialVisualFizzle(mob, null, failMessage(mob, auto));
			return false;
		}

		if(currentShift != null)
		{
			final String areaName = cloneArea.Name();
			final int x=areaName.indexOf('_');
			if((x>0)&&(CMath.isNumber(areaName.substring(0, x))))
			{
				final Area newCloneArea=CMLib.map().getArea(areaName.substring(x+1));
				if(newCloneArea!=null)
				{
					cloneArea=newCloneArea;
					if(cloneRoomID.startsWith(areaName)
					&&(cloneArea.getRoom(cloneRoomID.substring(x+1))!=null))
					{
						cloneRoomID=cloneRoomID.substring(x+1);
					}
					else
					{
						for(int i=0;i<100;i++)
						{
							final Room R=cloneArea.getRandomProperRoom();
							if((R!=null)
							&&(!CMLib.flags().isHidden(R))
							&&(CMLib.map().getExtendedRoomID(R).length()>0))
							{
								cloneRoomID=CMLib.map().getExtendedRoomID(R);
								break;
							}
						}
					}
				}
			}
		}

		boolean randomPlane=false;
		boolean randomTransitionPlane=false;
		boolean randomArea=alwaysRandomArea;
		if(((cloneArea.flags()&Area.FLAG_INSTANCE_CHILD)==Area.FLAG_INSTANCE_CHILD)
		&&(currentShift == null))
			randomArea=true;
		if(!success)
		{
			if(CMLib.dice().rollPercentage()>5)
			{
				this.beneficialVisualFizzle(mob, null, failMessage(mob, auto));
				return false;
			}
			else
			{
				if(proficiency()<50)
				{
					randomPlane=true;
					randomArea=true;
				}
				else
				if(proficiency()<75)
				{
					randomTransitionPlane=true;
					randomArea=true;
				}
				else
				if(proficiency()<100)
				{
					randomTransitionPlane=true;
				}
				else
					randomArea=true;
			}
		}
		else
		if(proficiencyCheck(mob,-95,auto))
		{
			// kaplah!
		}
		else
		if(proficiencyCheck(mob,-50,auto))
		{
			if(proficiency()<75)
			{
				randomTransitionPlane=true;
				randomArea=true;
			}
			else
			if(proficiency()<100)
			{
				randomTransitionPlane=true;
			}
		}
		else
		{
			if(proficiency()<75)
			{
				randomTransitionPlane=true;
				randomArea=true;
			}
			else
			if(proficiency()<100)
			{
				randomArea=true;
			}
			else
				randomTransitionPlane=true;
		}

		final List<String> transitionalPlaneKeys = getTransitionPlaneKeys();
		if(currentShift!=null)
		{
			if(transitionalPlaneKeys.contains(currentShift.text().toUpperCase().trim()))
			{
				if(randomTransitionPlane)
					randomTransitionPlane=false;
				else
				if(randomPlane)
				{
					randomPlane=false;
					randomTransitionPlane=true;
					randomArea=true;
				}
			}
		}

		if(randomArea)
		{
			int tries=0;
			while(((++tries)<10000))
			{
				final Room room=CMLib.map().getRandomRoom();
				if((room!=null)
				&&(CMLib.flags().canAccess(mob,room))
				&&(CMLib.map().getExtendedRoomID(room).length()>0)
				&&(room.getArea().numberOfProperIDedRooms()>2))
				{
					cloneArea=room.getArea();
					cloneRoomID=CMLib.map().getExtendedRoomID(room);
					break;
				}
			}
		}
		if(randomTransitionPlane)
		{
			planeName = transitionalPlaneKeys.get(CMLib.dice().roll(1, transitionalPlaneKeys.size(), -1));
			planeFound = getPlane(planeName);
		}
		if(randomPlane)
		{
			final List<String> allPlaneKeys = getAllPlaneKeys();
			planeName = allPlaneKeys.get(CMLib.dice().roll(1, allPlaneKeys.size(), -1));
			planeFound = getPlane(planeName);
		}

		final String planeCodeString = planeName + "_" + cloneArea.Name();
		int hardBumpLevel = 0;
		if(recentVisits.containsKey(planeCodeString)
		&&((recentVisits.get(planeCodeString)[0]+hardBumpTimeout)>System.currentTimeMillis()))
		{
			final long[] data = this.recentVisits.get(planeCodeString);
			data[0]=System.currentTimeMillis();
			if(data[1]==0)
				data[1]++;
			else
				data[1]*=2;
			hardBumpLevel=(int)data[1];
		}
		else
			this.recentVisits.put(planeCodeString, new long[] {System.currentTimeMillis(),0});
		final String newPlaneName = planeIDNum.addAndGet(1)+"_"+cloneArea.Name();
		final Area planeArea = CMClass.getAreaType("SubThinInstance");
		planeArea.setName(newPlaneName);
		planeArea.addBlurbFlag("PLANEOFEXISTENCE {"+planeName+"}");
		CMLib.map().addArea(planeArea);
		planeArea.setAreaState(Area.State.ACTIVE); // starts ticking
		final Room target=CMClass.getLocale("StdRoom");
		String newRoomID=this.convertToMyArea(newPlaneName,cloneRoomID);
		if(newRoomID==null)
			newRoomID=cloneRoomID;
		target.setRoomID(newRoomID);
		target.setDisplayText("Between The Planes of Existence");
		target.setDescription("You are a floating consciousness between the planes of existence...");
		target.setArea(planeArea);

		//CMLib.map().delArea(this.planeArea);

		final CMMsg msg=CMClass.getMsg(mob,target,this,CMMsg.MASK_MOVE|verbalCastCode(mob,target,auto),castingMessage(mob, auto));
		if((mob.location().okMessage(mob,msg))&&(target.okMessage(mob,msg)))
		{
			mob.location().send(mob,msg);
			final List<MOB> h=properTargetList(mob,givenTarget,false);
			if(h==null)
				return false;

			this.lastCasting=System.currentTimeMillis();
			final PlanarAbility A=(PlanarAbility)this.beneficialAffect(mob, planeArea, asLevel, 0);
			if(A!=null)
			{
				A.hardBumpLevel = hardBumpLevel;
				A.setMiscText(planeName);
			}

			final Room thisRoom=mob.location();
			for (final MOB follower : h)
			{
				final CMMsg enterMsg=CMClass.getMsg(follower,target,this,CMMsg.MSG_ENTER,null,CMMsg.MSG_ENTER,null,CMMsg.MSG_ENTER,("<S-NAME> fade(s) into view.")+CMLib.protocol().msp("appear.wav",10));
				final CMMsg leaveMsg=CMClass.getMsg(follower,thisRoom,this,CMMsg.MSG_LEAVE|CMMsg.MASK_MAGIC,L("<S-NAME> fade(s) away."));
				if(thisRoom.okMessage(follower,leaveMsg)&&target.okMessage(follower,enterMsg))
				{
					if(follower.isInCombat())
					{
						CMLib.commands().postFlee(follower,("NOWHERE"));
						follower.makePeace(false);
					}
					thisRoom.send(follower,leaveMsg);
					((Room)enterMsg.target()).bringMobHere(follower,false);
					follower.tell(L("\n\r\n\r"));
					((Room)enterMsg.target()).send(follower,enterMsg);
					CMLib.commands().postLook(follower,true);
				}
				else
				if(follower==mob)
					break;
			}
			planeArea.addBlurbFlag("PLANEOFEXISTENCE {"+planeName+"}");
		}
		// return whether it worked
		return success;
	}
}
