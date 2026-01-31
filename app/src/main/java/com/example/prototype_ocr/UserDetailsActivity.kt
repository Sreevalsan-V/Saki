package com.example.prototype_ocr

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.prototype_ocr.api.AuthRepository
import com.google.android.material.navigation.NavigationView

class UserDetailsActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var authRepository: AuthRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_details_new)
        
        authRepository = AuthRepository(this)
        
        // Check if logged in
        if (!authRepository.isLoggedIn()) {
            navigateToLogin()
            return
        }
        
        // Setup toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        
        // Setup drawer
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        navigationView.setNavigationItemSelectedListener(this)
        
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        
        // Setup navigation header
        setupNavigationHeader()
        
        // Profile button
        findViewById<ImageButton>(R.id.profileButton).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
        
        // IQC and EQAS buttons
        val iqcButton = findViewById<Button>(R.id.iqcButton)
        val eqasButton = findViewById<Button>(R.id.eqasButton)
        
        iqcButton.setOnClickListener {
            // TODO: Implement IQC functionality
        }
        
        eqasButton.setOnClickListener {
            val intent = Intent(this, DeviceSelectionActivity::class.java)
            startActivity(intent)
        }
    }
    
    private fun setupNavigationHeader() {
        val headerView = navigationView.getHeaderView(0)
        val userData = authRepository.getCachedUserData()
        
        userData?.let {
            headerView.findViewById<TextView>(R.id.navHeaderName).text = it.name
            headerView.findViewById<TextView>(R.id.navHeaderUsername).text = it.username
        }
    }
    
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_view_records -> {
                startActivity(Intent(this, RecordsActivity::class.java))
            }
            R.id.nav_profile -> {
                startActivity(Intent(this, ProfileActivity::class.java))
            }
            R.id.nav_logout -> {
                showLogoutDialog()
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }
    
    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun performLogout() {
        authRepository.logout()
        navigateToLogin()
    }
    
    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    
    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}
