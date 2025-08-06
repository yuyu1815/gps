package com.example.myapplication.domain.usecase.map

import com.example.myapplication.data.repository.IMapRepository
import com.example.myapplication.domain.model.IndoorMap
import com.example.myapplication.domain.usecase.UseCase
import timber.log.Timber

/**
 * Use case for loading maps from a directory.
 * This use case discovers map files in the specified directory and loads them.
 */
class LoadMapsFromDirectoryUseCase(
    private val mapRepository: IMapRepository
) : UseCase<LoadMapsFromDirectoryUseCase.Params, List<IndoorMap>> {
    
    override suspend fun invoke(params: Params): List<IndoorMap> {
        Timber.d("Loading maps from directory: ${params.directoryPath}")
        
        return try {
            // Discover and load maps from the directory
            val maps = mapRepository.loadAllMapsFromDirectory(params.directoryPath)
            
            // Set the first map as active if requested and maps were loaded
            if (params.setFirstMapActive && maps.isNotEmpty()) {
                mapRepository.setActiveMap(maps.first().id)
                Timber.d("Set first map as active: ${maps.first().id}")
            }
            
            maps
        } catch (e: Exception) {
            Timber.e(e, "Error loading maps from directory: ${params.directoryPath}")
            emptyList()
        }
    }
    
    /**
     * Parameters for the LoadMapsFromDirectoryUseCase.
     *
     * @param directoryPath Path to the directory containing map files
     * @param setFirstMapActive Whether to set the first loaded map as active
     */
    data class Params(
        val directoryPath: String,
        val setFirstMapActive: Boolean = true
    )
}