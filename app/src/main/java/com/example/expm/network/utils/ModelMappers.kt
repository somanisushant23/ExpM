package com.example.expm.network.utils

import com.example.expm.data.Entry
import com.example.expm.network.models.EntryData
import com.example.expm.network.models.EntryRequest

/**
 * Extension functions to convert between local and API models
 */

// Convert local Entry to API EntryRequest
fun Entry.toEntryRequest(): EntryRequest {
    return EntryRequest(
        title = this.title,
        amount = this.amount,
        type = this.type,
        category = this.category,
        createdOn = this.created_on,
        updatedOn = this.updated_on,
        notes = this.notes
    )
}

// Convert API EntryData to local Entry
fun EntryData.toEntry(): Entry {
    return Entry(
        title = this.title,
        amount = this.amount,
        type = this.type,
        category = this.category,
        created_on = this.createdOn,
        updated_on = this.updatedOn,
        notes = this.notes,
        isPersisted = this.isPersisted,
        remoteId = this.id
    )
}

