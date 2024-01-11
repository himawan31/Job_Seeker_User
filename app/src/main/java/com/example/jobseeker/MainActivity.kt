package com.example.jobseeker

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.jobseeker.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        replaceFragment(HomeFragment())

        binding.bottomNav.setOnItemSelectedListener {
            when(it.itemId){
                R.id.homeFragment -> replaceFragment(HomeFragment())
                R.id.searchFragment -> replaceFragment(SearchFragment())
                R.id.savedFragment -> replaceFragment(SavedFragment())
                R.id.profileFragment -> replaceFragment(ProfileFragment())
                R.id.addMenu -> {
                    // Memunculkan BottomSheetFragment saat item "addMenu" di bottomNav dipilih
                    val bottomSheetFragment = BottomSheetButtonPlusFragment()
                    bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)
                }

                else->{

                }
            }
            true
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.bottom_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.addMenu -> {
                // Memunculkan BottomSheetFragment saat tombol tambah ditekan
                val bottomSheetFragment = BottomSheetButtonPlusFragment()
                bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun replaceFragment(fragment: Fragment){
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.fragmenContainer, fragment)
        fragmentTransaction.commit()
    }
}