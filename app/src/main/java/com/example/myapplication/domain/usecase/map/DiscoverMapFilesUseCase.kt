package com.example.myapplication.domain.usecase.map

import com.example.myapplication.data.repository.IMapRepository
import com.example.myapplication.domain.usecase.UseCase
import timber.log.Timber

/**
 * Use case for discovering map files in a directory.
 * This use case finds all JSON files in the specified directory that could be map files.
 */
class DiscoverMapFilesUseCase(
    private val mapRepository: IMapRepository
) : UseCase<DiscoverMapFilesUseCase.Params, List<String>> {
    
    override suspend fun invoke(params: Params): List<String> {
        Timber.d("Discovering map files in directory: ${params.directoryPath}")
        
        return try {
            // Discover map files in the directory
            val mapFiles = mapRepository.discoverMapFiles(params.directoryPath)
            
            // Filter files if a filter is provided
            val filteredFiles = if (params.fileNameFilter != null) {
                val filter = params.fileNameFilter.lowercase()
                mapFiles.filter { filePath ->
                    val fileName = filePath.substringAfterLast('/', filePath.substringAfterLast('\\'))
                    fileName.lowercase().contains(filter)
                }
            } else {
                mapFiles
            }
            
            Timber.d("Discovered ${filteredFiles.size} map files")
            filteredFiles
        } catch (e: Exception) {
            Timber.e(e, "Error discovering map files in directory: ${params.directoryPath}")
            emptyList()
        }
    }
    
    /**
     * Parameters for the DiscoverMapFilesUseCase.
     *
     * @param directoryPath Path to the directory to search for map files
     * @param fileNameFilter Optional filter for file names (case-insensitive)
     */
    data class Params(
        val directoryPath: String,
        val fileNameFilter: String? = null
    )
}