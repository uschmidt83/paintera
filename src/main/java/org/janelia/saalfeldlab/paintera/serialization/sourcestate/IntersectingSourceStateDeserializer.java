package org.janelia.saalfeldlab.paintera.serialization.sourcestate;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Type;
import java.util.concurrent.ExecutorService;
import java.util.function.IntFunction;
import java.util.function.Supplier;

import org.janelia.saalfeldlab.paintera.cache.global.GlobalCache;
import org.janelia.saalfeldlab.paintera.cache.global.InvalidAccessException;
import org.janelia.saalfeldlab.paintera.composition.Composite;
import org.janelia.saalfeldlab.paintera.serialization.StatefulSerializer;
import org.janelia.saalfeldlab.paintera.serialization.StatefulSerializer.Arguments;
import org.janelia.saalfeldlab.paintera.state.IntersectingSourceState;
import org.janelia.saalfeldlab.paintera.state.LabelSourceState;
import org.janelia.saalfeldlab.paintera.state.SourceState;
import org.janelia.saalfeldlab.paintera.state.ThresholdingSourceState;
import org.janelia.saalfeldlab.paintera.viewer3d.ViewFrustum;
import org.janelia.saalfeldlab.util.concurrent.PriorityExecutorService;
import org.scijava.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import javafx.beans.property.ObjectProperty;
import javafx.scene.Group;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;

public class IntersectingSourceStateDeserializer implements JsonDeserializer<IntersectingSourceState>
{

	private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	public static final String NAME_KEY = "name";

	public static final String COMPOSITE_KEY = "composite";

	public static final String COMPOSITE_TYPE_KEY = "compositeType";

	private final IntFunction<SourceState<?, ?>> dependsOn;

	private final GlobalCache globalCache;

	private final int priority;

	private final Group meshesGroup;

	private final ObjectProperty<ViewFrustum> viewFrustumProperty;

	private final ObjectProperty<AffineTransform3D> eyeToWorldTransformProperty;

	private final ExecutorService manager;

	private final PriorityExecutorService workers;

	public IntersectingSourceStateDeserializer(
			final IntFunction<SourceState<?, ?>> dependsOn,
			final GlobalCache globalCache,
			final int priority,
			final Group meshesGroup,
			final ObjectProperty<ViewFrustum> viewFrustumProperty,
			final ObjectProperty<AffineTransform3D> eyeToWorldTransformProperty,
			final ExecutorService manager,
			final PriorityExecutorService workers)
	{
		super();
		this.dependsOn = dependsOn;
		this.globalCache = globalCache;
		this.priority = priority;
		this.meshesGroup = meshesGroup;
		this.viewFrustumProperty = viewFrustumProperty;
		this.eyeToWorldTransformProperty = eyeToWorldTransformProperty;
		this.manager = manager;
		this.workers = workers;
	}

	@Plugin(type = StatefulSerializer.DeserializerFactory.class)
	public static class Factory
			implements StatefulSerializer.DeserializerFactory<IntersectingSourceState, IntersectingSourceStateDeserializer>
	{

		@Override
		public IntersectingSourceStateDeserializer createDeserializer(
				final Arguments arguments,
				final Supplier<String> projectDirectory,
				final IntFunction<SourceState<?, ?>> dependencyFromIndex)
		{
			return new IntersectingSourceStateDeserializer(
					dependencyFromIndex,
					arguments.globalCache,
					0,
					arguments.viewer.viewer3D().meshesGroup(),
					arguments.viewer.viewer3D().viewFrustumProperty(),
					arguments.viewer.viewer3D().eyeToWorldTransformProperty(),
					arguments.meshManagerExecutors,
					arguments.meshWorkersExecutors
			);
		}

		@Override
		public Class<IntersectingSourceState> getTargetClass() {
			return IntersectingSourceState.class;
		}
	}

	@Override
	public IntersectingSourceState deserialize(final JsonElement el, final Type type, final JsonDeserializationContext
			context)
	throws JsonParseException
	{
		final JsonObject map = el.getAsJsonObject();
		LOG.debug("Deserializing {}", map);
		final int[] dependsOn = context.deserialize(map.get(SourceStateSerialization.DEPENDS_ON_KEY), int[].class);

		if (dependsOn.length != 2)
		{
			throw new JsonParseException("Expected exactly three dependency, got: " + map.get(SourceStateSerialization
					.DEPENDS_ON_KEY));
		}

		final SourceState<?, ?> thresholdedState = this.dependsOn.apply(dependsOn[0]);
		final SourceState<?, ?> labelState       = this.dependsOn.apply(dependsOn[1]);
		if (thresholdedState == null || labelState == null) { return null; }

		if (!(thresholdedState instanceof ThresholdingSourceState<?, ?>))
		{
			throw new JsonParseException("Expected " + ThresholdingSourceState.class.getName() + " as second " +
					"dependency but got " + thresholdedState.getClass().getName() + " instead.");
		}

		if (!(labelState instanceof LabelSourceState<?, ?>))
		{
			throw new JsonParseException("Expected " + LabelSourceState.class.getName() + " as third dependency but " +
					"got " + labelState.getClass().getName() + " instead.");
		}

		try
		{
			final Class<? extends Composite<ARGBType, ARGBType>> compositeType = (Class<Composite<ARGBType,
					ARGBType>>) Class.forName(
					map.get(COMPOSITE_TYPE_KEY).getAsString());
			final Composite<ARGBType, ARGBType>                  composite     = context.deserialize(map.get(
					COMPOSITE_KEY), compositeType);

			final String name = map.get(NAME_KEY).getAsString();


			LOG.debug(
					"Creating {} with thresholded={} labels={}",
					IntersectingSourceState.class.getSimpleName(),
					thresholdedState,
					labelState
			         );
			final IntersectingSourceState state = new IntersectingSourceState(
					(ThresholdingSourceState) thresholdedState,
					(LabelSourceState) labelState,
					composite,
					name,
					globalCache,
					priority,
					meshesGroup,
					viewFrustumProperty,
					eyeToWorldTransformProperty,
					manager,
					workers
			);

			return state;
		} catch (final ClassNotFoundException | InvalidAccessException e)
		{
			throw new JsonParseException(e);
		}
	}

}
