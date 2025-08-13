package com.example.myapplication.data.repository

/**
 * Base repository interface that defines common operations for all repositories.
 * This interface is used to enforce a consistent pattern across all repositories.
 */
interface IRepository<T> {
    /**
     * Gets all items from the repository.
     * @return List of items
     */
    suspend fun getAll(): List<T>
    
    /**
     * Gets an item by its ID.
     * @param id The ID of the item to get
     * @return The item, or null if not found
     */
    suspend fun getById(id: String): T?
    
    /**
     * Saves an item to the repository.
     * @param item The item to save
     * @return True if the operation was successful, false otherwise
     */
    suspend fun save(item: T): Boolean
    
    /**
     * Updates an existing item in the repository.
     * @param item The item to update
     * @return True if the operation was successful, false otherwise
     */
    suspend fun update(item: T): Boolean
    
    /**
     * Deletes an item from the repository.
     * @param id The ID of the item to delete
     * @return True if the operation was successful, false otherwise
     */
    suspend fun delete(id: String): Boolean
}