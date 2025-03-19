package com.example.criminalintent.callbacks

import java.util.UUID

interface CrimeCallback {
    fun onCrimeSelected(crimeId: UUID)

}