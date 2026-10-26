package com.abit.multimedia

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.MenuProvider
import androidx.core.view.ViewCompat
import androidx.core.view.ViewGroupCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.onNavDestinationSelected
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.abit.multimedia.databinding.ActivityMainBinding
import com.abit.multimedia.databinding.NavHeaderMainBinding
import com.abit.multimedia.ui.applySystemBarInsetsAsPadding

/**
 * Hosts the navigation drawer and the four feature screens, which are Fragments.
 *
 * The Navigation Component does most of the work:
 *  - NavHostFragment swaps the feature Fragments in and out (res/navigation/nav_graph.xml).
 *  - NavigationUI connects the drawer and the toolbar to the NavController, so the toolbar
 *    title, the hamburger icon and the highlighted drawer item always match the current screen.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

    /** Closes the drawer when Back is pressed. It is only enabled while the drawer is open. */
    private val closeDrawerOnBack = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }
    }

    /** Toolbar overflow menu with the About item (About is not one of the four drawer items). */
    private val overflowMenu = object : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.main_overflow, menu)
        }

        // The menu item ID equals the destination ID, so NavigationUI can open the screen.
        override fun onMenuItemSelected(menuItem: MenuItem): Boolean =
            menuItem.onNavDestinationSelected(navController)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        applyWindowInsets()

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // The four drawer screens are top-level destinations, so they show the hamburger icon
        // instead of an Up arrow. Their IDs are the same as the drawer menu item IDs.
        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.nav_broadcast, R.id.nav_image_scale, R.id.nav_video, R.id.nav_audio),
            binding.drawerLayout,
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.navView.setupWithNavController(navController)

        setUpDrawerHeader()
        setUpBackHandling()
        addMenuProvider(overflowMenu, this)
    }

    // Called when the hamburger icon or the Up arrow in the toolbar is tapped.
    override fun onSupportNavigateUp(): Boolean =
        navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        // After a rotation the drawer can be restored already open without onDrawerOpened()
        // being called, so sync the Back callback with the drawer's real state here.
        closeDrawerOnBack.isEnabled = binding.drawerLayout.isDrawerOpen(GravityCompat.START)
    }

    private fun applyWindowInsets() {
        // Makes inset dispatching on old Android versions behave like Android 11+.
        ViewGroupCompat.installCompatInsetsDispatch(binding.root)
        binding.appBar.applySystemBarInsetsAsPadding(applyTop = true)
        binding.navHostFragment.applySystemBarInsetsAsPadding(applyBottom = true)
    }

    private fun setUpDrawerHeader() {
        val header = NavHeaderMainBinding.bind(binding.navView.getHeaderView(0))
        // The header is drawn behind the status bar, so move its content below it.
        header.root.applySystemBarInsetsAsPadding(applyTop = true)
        header.root.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            openAbout()
        }
        // Tells screen readers what tapping the header does.
        ViewCompat.replaceAccessibilityAction(
            header.root,
            AccessibilityActionCompat.ACTION_CLICK,
            getString(R.string.nav_header_open_about),
            null,
        )
    }

    private fun openAbout() {
        // launchSingleTop avoids stacking a second About screen on top of an existing one.
        navController.navigate(R.id.nav_about, null, navOptions { launchSingleTop = true })
    }

    private fun setUpBackHandling() {
        // Registered after the NavHostFragment's own callback, so it gets Back first.
        onBackPressedDispatcher.addCallback(this, closeDrawerOnBack)
        binding.drawerLayout.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
            override fun onDrawerOpened(drawerView: View) {
                closeDrawerOnBack.isEnabled = true
            }

            override fun onDrawerClosed(drawerView: View) {
                closeDrawerOnBack.isEnabled = false
            }
        })
    }
}
