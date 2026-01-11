package com.example.expm.adapter

import com.example.expm.network.models.InvestmentResponse

sealed class InvestmentGroupedItem {
    data class Header(val investmentType: String) : InvestmentGroupedItem()
    data class Item(val investment: InvestmentResponse) : InvestmentGroupedItem()
}

