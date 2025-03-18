package com.example.criminalintent.features.blacklist.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.criminalintent.model.Crime

data class CrimeState(
    val crimesList: LiveData<List<Crime>> = MutableLiveData()
)
