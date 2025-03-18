package com.example.criminalintent.features.blacklist.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.criminalintent.features.blacklist.model.CrimeState
import com.example.criminalintent.model.Crime
import com.example.criminalintent.repository.CrimeRepository

class CrimeListViewModel : ViewModel() {
    private val crimeRepository = CrimeRepository.get()

    private val _state = MutableLiveData(CrimeState())
    val state: LiveData<CrimeState> get() = _state

    init {
        load()
    }

    private fun load() {
        _state.value = _state.value?.copy(
            crimesList = crimeRepository.getCrimes()
        )
    }
    fun addCrime(crime: Crime) {
        crimeRepository.addCrime(crime)
    }


}