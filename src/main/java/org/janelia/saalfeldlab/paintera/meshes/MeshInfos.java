package org.janelia.saalfeldlab.paintera.meshes;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.janelia.saalfeldlab.paintera.control.assignment.FragmentSegmentAssignment;
import org.janelia.saalfeldlab.paintera.control.selection.SelectedSegments;

import javafx.beans.InvalidationListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class MeshInfos<T>
{
	private final ObservableList<MeshInfo<T>> infos = FXCollections.observableArrayList();

	private final ObservableList<MeshInfo<T>> readOnlyInfos = FXCollections.unmodifiableObservableList(infos);

	private final ManagedMeshSettings meshSettings;

	public MeshInfos(
			final SelectedSegments selectedSegments,
			final FragmentSegmentAssignment assignment,
			final MeshManager<Long, T> meshManager,
			final ManagedMeshSettings meshSettings,
			final int numScaleLevels)
	{
		super();

		this.meshSettings = meshSettings;

		final InvalidationListener updateMeshInfosHandler = obs -> {
			final long[] segments = selectedSegments.getSelectedSegments();
			if (!meshSettings.isMeshListEnabledProperty().get())
			{
				this.infos.clear();
				return;
			}

			final List<MeshInfo<T>> infos = Arrays
					.stream(segments)
					.mapToObj(id -> new MeshInfo<>(
							id,
							meshSettings.getOrAddMesh(id),
							meshSettings.isManagedProperty(id),
							assignment,
							meshManager
					))
					.collect(Collectors.toList());

			this.infos.forEach(MeshInfo::hangUp);
			this.infos.setAll(infos);
		};

		selectedSegments.addListener(updateMeshInfosHandler);
		meshSettings.isMeshListEnabledProperty().addListener(updateMeshInfosHandler);
	}

	public ObservableList<MeshInfo<T>> readOnlyInfos()
	{
		return this.readOnlyInfos;
	}

	public ManagedMeshSettings meshSettings()
	{
		return meshSettings;
	}
}
