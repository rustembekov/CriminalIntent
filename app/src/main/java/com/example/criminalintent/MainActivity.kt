package com.example.criminalintent

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.criminalintent.callbacks.CrimeCallback
import com.example.criminalintent.features.blacklist.view.CrimeListFragment
import com.example.criminalintent.features.crime.view.CrimeFragment
import java.util.UUID

private const val TAG = "MainActivity"
private const val BLANK_FRAGMENT_TAG = "BLANK_FRAGMENT_TAG"
class MainActivity : AppCompatActivity(), CrimeCallback {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if(currentFragment == null) {
            val fragment = CrimeListFragment.newInstance()
            supportFragmentManager
                .beginTransaction()
                .add(R.id.fragment_container, fragment)
                .commit()
        }

    }

    override fun onCrimeSelected(crimeId: UUID) {
        val fragmentManager = supportFragmentManager

        val blankFragment = fragmentManager.findFragmentByTag(BLANK_FRAGMENT_TAG)
        if (blankFragment != null) {
            fragmentManager.beginTransaction().remove(blankFragment).commit()
        }

        val fragment = CrimeFragment.newInstance(crimeId)
        fragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

}
