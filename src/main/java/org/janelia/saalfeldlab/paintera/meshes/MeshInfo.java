package org.janelia.saalfeldlab.paintera.meshes;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

import org.janelia.saalfeldlab.paintera.control.assignment.FragmentSegmentAssignment;
import org.janelia.saalfeldlab.paintera.control.assignment.FragmentSegmentAssignmentState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableIntegerValue;
import javafx.beans.value.ObservableValue;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;

public class MeshInfo< T >
{

	private static final Logger LOG = LoggerFactory.getLogger( MethodHandles.lookup().lookupClass() );

	private final IntegerProperty scaleLevel = new SimpleIntegerProperty();

	private final IntegerProperty simplificationIterations = new SimpleIntegerProperty();

	private final DoubleProperty smoothingLambda = new SimpleDoubleProperty();

	private final IntegerProperty smoothingIterations = new SimpleIntegerProperty();

	private final Long segmentId;

	private final FragmentSegmentAssignment assignment;

	private final MeshManager< Long, T > meshManager;

	private final int numScaleLevels;

	private final IntegerProperty submittedTasks = new SimpleIntegerProperty( 0 );

	private final IntegerProperty completedTasks = new SimpleIntegerProperty( 0 );

	private final IntegerProperty successfulTasks = new SimpleIntegerProperty( 0 );

	private final DoubleProperty opacity = new SimpleDoubleProperty( 1.0 );

	private final ObjectProperty< DrawMode > drawMode = new SimpleObjectProperty<>( DrawMode.FILL );

	private final ObjectProperty< CullFace > cullFace = new SimpleObjectProperty<>( CullFace.FRONT );

	private final PropagateChanges< Number > scaleLevelListener = new PropagateChanges<>( ( mesh, newv ) -> mesh.scaleIndexProperty().set( newv.intValue() ) );

	private final PropagateChanges< Number > simplificationIterationsListener = new PropagateChanges<>( ( mesh, newv ) -> mesh.meshSimplificationIterationsProperty().set( newv.intValue() ) );

	private final PropagateChanges< Number > smoothingLambdaListener = new PropagateChanges<>( ( mesh, newv ) -> mesh.smoothingLambdaProperty().set( newv.doubleValue() ) );

	private final PropagateChanges< Number > opacityListener = new PropagateChanges<>( ( mesh, newv ) -> mesh.opacityProperty().set( newv.doubleValue() ) );

	private final PropagateChanges< Number > smoothingIterationsListener = new PropagateChanges<>( ( mesh, newv ) -> mesh.smoothingIterationsProperty().set( newv.intValue() ) );

	private final PropagateChanges< DrawMode > drawModeListener = new PropagateChanges<>( ( mesh, newv ) -> mesh.drawModeProperty().set( newv ) );

	private final PropagateChanges< CullFace > cullFaceListener = new PropagateChanges<>( ( mesh, newv ) -> mesh.cullFaceProperty().set( newv ) );

	public MeshInfo(
			final Long segmentId,
			final FragmentSegmentAssignment assignment,
			final MeshManager< Long, T > meshManager,
			final int numScaleLevels )
	{
		super();
		this.segmentId = segmentId;
		this.assignment = assignment;
		this.meshManager = meshManager;

		scaleLevel.set( meshManager.scaleLevelProperty().get() );
		simplificationIterations.set( meshManager.meshSimplificationIterationsProperty().get() );
		smoothingLambda.set( meshManager.smoothingLambdaProperty().get() );
		smoothingIterations.set( meshManager.smoothingIterationsProperty().get() );

		listen();

		this.numScaleLevels = numScaleLevels;

		updateTasksCountBindings();
		if ( assignment instanceof FragmentSegmentAssignmentState )
		{
			( ( FragmentSegmentAssignmentState ) assignment ).addListener( obs -> updateTasksCountBindings() );
		}

	}

	public void listen()
	{
		this.scaleLevel.addListener( this.scaleLevelListener );
		this.simplificationIterations.addListener( this.simplificationIterationsListener );
		this.smoothingLambda.addListener( this.smoothingLambdaListener );
		this.opacity.addListener( this.opacityListener );
		this.smoothingIterations.addListener( this.smoothingIterationsListener );
		this.drawMode.addListener( this.drawModeListener );
		this.cullFace.addListener( this.cullFaceListener );
	}

	public void hangUp()
	{
		this.scaleLevel.removeListener( this.scaleLevelListener );
		this.simplificationIterations.removeListener( this.simplificationIterationsListener );
		this.smoothingLambda.removeListener( this.smoothingLambdaListener );
		this.opacity.removeListener( this.opacityListener );
		this.smoothingIterations.removeListener( this.smoothingIterationsListener );
		this.drawMode.removeListener( this.drawModeListener );
		this.cullFace.removeListener( this.cullFaceListener );
	}

	private void updateTasksCountBindings()
	{
		LOG.debug( "Updating task count bindings." );
		final Map< Long, MeshGenerator< T > > meshes = new HashMap<>( meshManager.unmodifiableMeshMap() );
		LOG.debug( "Binding meshes to segmentId = {}", segmentId );
		Optional.ofNullable( meshes.get( segmentId ) ).map( MeshGenerator::submittedTasksProperty ).ifPresent( this.submittedTasks::bind );
		Optional.ofNullable( meshes.get( segmentId ) ).map( MeshGenerator::completedTasksProperty ).ifPresent( this.completedTasks::bind );
		Optional.ofNullable( meshes.get( segmentId ) ).map( MeshGenerator::successfulTasksProperty ).ifPresent( this.successfulTasks::bind );
	}

	public Long segmentId()
	{
		return this.segmentId;
	}

	public IntegerProperty scaleLevelProperty()
	{
		return this.scaleLevel;
	}

	public IntegerProperty simplificationIterationsProperty()
	{
		return this.simplificationIterations;
	}

	public DoubleProperty smoothingLambdaProperty()
	{
		return this.smoothingLambda;
	}

	public IntegerProperty smoothingIterationsProperty()
	{
		return this.smoothingIterations;
	}

	public FragmentSegmentAssignment assignment()
	{
		return this.assignment;
	}

	public int numScaleLevels()
	{
		return this.numScaleLevels;
	}

	public DoubleProperty opacityProperty()
	{
		return this.opacity;
	}

	private class PropagateChanges< U > implements ChangeListener< U >
	{

		final BiConsumer< MeshGenerator< T >, U > apply;

		public PropagateChanges( final BiConsumer< MeshGenerator< T >, U > apply )
		{
			super();
			this.apply = apply;
		}

		@Override
		public void changed( final ObservableValue< ? extends U > observable, final U oldValue, final U newValue )
		{
			final Map< Long, MeshGenerator< T > > meshes = meshManager.unmodifiableMeshMap();
			apply.accept( meshes.get( segmentId ), newValue );
		}

	}

	@Override
	public int hashCode()
	{
		return segmentId.hashCode();
	}

	@Override
	public boolean equals( final Object o )
	{
		return o instanceof MeshInfo< ? > && ( ( MeshInfo< ? > ) o ).segmentId == segmentId;
	}

	public ObservableIntegerValue submittedTasksProperty()
	{
		return this.submittedTasks;
	}

	public ObservableIntegerValue completedTasksProperty()
	{
		return this.completedTasks;
	}

	public ObservableIntegerValue successfulTasksProperty()
	{
		return this.successfulTasks;
	}

	public MeshManager< Long, T > meshManager()
	{
		return this.meshManager;
	}

	public ObjectProperty< DrawMode > drawModeProperty()
	{
		return this.drawMode;
	}

	public ObjectProperty< CullFace > cullFaceProperty()
	{
		return this.cullFace;
	}

	public long[] containedFragments()
	{
		return meshManager.containedFragments( segmentId );
	}

}
