package com.teamwizardry.refraction.common.light;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import com.google.common.collect.HashMultimap;

public class ReflectionTracker
{
	private static WeakHashMap<WeakReference<World>, ReflectionTracker> instances = new WeakHashMap<WeakReference<World>, ReflectionTracker>();
	private static HashMultimap<ILightSource, Beam> sourceBlocks;
	private static HashMultimap<ILightSink, Beam> sinkBlocks;
	private static HashMap<Beam, ILightSource> sources;
	private static HashMap<Beam, ILightSink> sinks;

	public ReflectionTracker()
	{
		sourceBlocks = HashMultimap.create();
		sinkBlocks = HashMultimap.create();
		sources = new HashMap<>();
		sinks = new HashMap<>();
	}

	/**
	 * Adds the given {@code ILightSink} to the beam's end. Additionally adds
	 * the beam to the list of beams focused at said TileEntity.
	 * 
	 * @param sink
	 *            The {@link ILightSink} being targeted by the beam
	 * @param beam
	 *            The {@link Beam} focusing the given TileEntity
	 */
	public void recieveBeam(ILightSink sink, Beam beam)
	{
		if (sinks.putIfAbsent(beam, sink) == null)
			sinkBlocks.put(sink, beam);
	}

	/**
	 * Adds the given {@code ILightSource} to the beam's start. Additionally
	 * adds the beam to the list of beams originating from said TileEntity. Will
	 * then run beam detection code for any TileEntity at the other end of the
	 * beam.
	 * 
	 * @param source
	 *            The {@link ILightSource} generating the beam
	 * @param beam
	 *            The {@link Beam} generated by the given TileEntity
	 */
	public void generateBeam(ILightSource source, Beam beam)
	{
		if (sources.putIfAbsent(beam, source) == null)
		{
			sourceBlocks.put(source, beam);
			BlockTracker.addBeam(beam);
			TileEntity entity = beam.world.getTileEntity(new BlockPos(beam.finalLoc));
			if (entity != null && entity instanceof ILightSink)
			{
				((ILightSink) entity).recieveBeam(beam);
			}
		}
	}

	/**
	 * Removes all references to the given Beam from the tracker. Causes all
	 * child beams to be recalculated.
	 * 
	 * @param beam
	 *            The beam to remove
	 */
	public void disableBeam(Beam beam)
	{
		ILightSource source = sources.remove(beam);
		ILightSink sink = sinks.remove(beam);
		sourceBlocks.remove(source, beam);
		sinkBlocks.remove(sink, beam);
		if (sink instanceof ILightSource)
		{
			for (Beam child : sourceBlocks.get((ILightSource) sink))
				disableBeam(child);
		}
		Set<Beam> sinkBeams = sinkBlocks.get(sink);
		sink.recieveBeam(sinkBeams.toArray(new Beam[sinkBeams.size()]));
	}

	public Set<Beam> getRecievedBeams(ILightSink sink)
	{
		return sinkBlocks.get(sink);
	}

	public static ReflectionTracker getInstance(World world)
	{
		return instances.get(world);
	}
	
	public static boolean addInstance(World world)
	{
		return instances.putIfAbsent(new WeakReference<World>(world), new ReflectionTracker()) == null;
	}
}
