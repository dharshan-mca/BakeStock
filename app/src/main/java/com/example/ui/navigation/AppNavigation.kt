package com.example.ui.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.model.UserRole
import com.example.ui.admin.*
import com.example.ui.auth.*
import com.example.ui.kitchen.*
import com.example.ui.delivery.*
import com.example.ui.shared.*
import com.example.ui.analytics.*
import com.example.ui.theme.BakeStockTheme

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Splash : Screen("splash", "Splash")
    object RoleSelection : Screen("role_selection", "Select Role")
    object Login : Screen("login/{role}", "Login") {
        fun createRoute(role: UserRole) = "login/${role.name}"
    }

    // Admin Screens
    object AdminDashboard : Screen("admin_dashboard", "Dashboard", Icons.Default.Dashboard)
    object ProductCatalog : Screen("product_catalog", "Inventory", Icons.Default.Inventory2)
    object AddEditProduct : Screen("add_edit_product?id={id}", "Product Form") {
        fun createRoute(id: String? = null) = if (id != null) "add_edit_product?id=$id" else "add_edit_product"
    }
    object ProductDetails : Screen("product_details/{id}", "Product Details") {
        fun createRoute(id: String) = "product_details/$id"
    }
    object Purchases : Screen("purchases", "Purchases", Icons.Default.ReceiptLong)
    object AddPurchase : Screen("add_purchase", "New Purchase")
    object Suppliers : Screen("suppliers", "Suppliers")
    object Staff : Screen("staff", "Staff")
    object Recipes : Screen("recipes", "Recipes")
    object Wastage : Screen("wastage", "Wastage")
    object Reports : Screen("reports", "Reports")
    object Analytics : Screen("analytics", "Analytics", Icons.Default.Analytics)
    object InventoryForecast : Screen("inventory_forecast", "Forecast", Icons.Default.TrendingUp)
    object FirebaseStatus : Screen("firebase_status", "Firebase Status", Icons.Default.CloudSync)

    // Kitchen Master Screens
    object KitchenDashboard : Screen("kitchen_dashboard", "Kitchen", Icons.Default.SoupKitchen)
    object ProductionPlan : Screen("production_plan", "Plan")
    object CreateBatch : Screen("create_batch", "Batch")
    object KitchenRequest : Screen("kitchen_request", "Requests")

    // Delivery Man Screens
    object DeliveryDashboard : Screen("delivery_dashboard", "Deliveries", Icons.Default.LocalShipping)
    object DeliveryUpdate : Screen("delivery_update/{id}", "Update Request") {
        fun createRoute(id: String) = "delivery_update/$id"
    }

    // Shared
    object Profile : Screen("profile", "Profile", Icons.Default.Person)
    object Notifications : Screen("notifications", "Alerts")
    object ActivityLogs : Screen("activity_logs", "Logs")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    authViewModel: AuthViewModel,
    navController: NavHostController = rememberNavController()
) {
    var isWhiteBackground by remember { mutableStateOf(false) }

    val currentUser by authViewModel.currentUser.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val repository = authViewModel.repository

    val showBottomBar = currentRoute in listOf(
        Screen.AdminDashboard.route,
        Screen.ProductCatalog.route,
        Screen.Purchases.route,
        Screen.KitchenDashboard.route,
        Screen.DeliveryDashboard.route,
        Screen.Profile.route
    )

    BakeStockTheme(isWhiteBackground = isWhiteBackground) {
        Scaffold(
            topBar = {
                if (showBottomBar) {
                    CenterAlignedTopAppBar(
                        title = {
                            Text(
                                text = when (currentUser?.role) {
                                    UserRole.ADMIN -> "BakeStock Admin"
                                    UserRole.KITCHEN_MASTER -> "Kitchen Workspace"
                                    UserRole.DELIVERY_MAN -> "Delivery Workspace"
                                    null -> "BakeStock"
                                },
                                fontWeight = FontWeight.Bold
                            )
                        },
                        actions = {
                            IconButton(
                                onClick = { isWhiteBackground = !isWhiteBackground },
                                modifier = Modifier.testTag("toggle_white_bg_button")
                            ) {
                                Icon(
                                    imageVector = if (isWhiteBackground) Icons.Default.Palette else Icons.Outlined.Palette,
                                    contentDescription = "Switch Background Color",
                                    tint = if (isWhiteBackground) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { navController.navigate(Screen.Notifications.route) },
                                modifier = Modifier.testTag("top_bar_notifications")
                            ) {
                                Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            },
        bottomBar = {
            if (showBottomBar && currentUser != null) {
                NavigationBar(
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    when (currentUser?.role) {
                        UserRole.ADMIN -> {
                            NavigationBarItem(
                                selected = currentRoute == Screen.AdminDashboard.route,
                                onClick = { navController.navigate(Screen.AdminDashboard.route) { popUpTo(navController.graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true } },
                                icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                                label = { Text("Dashboard") },
                                modifier = Modifier.testTag("tab_admin_dashboard")
                            )
                            NavigationBarItem(
                                selected = currentRoute == Screen.ProductCatalog.route,
                                onClick = { navController.navigate(Screen.ProductCatalog.route) { popUpTo(navController.graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true } },
                                icon = { Icon(Icons.Default.Inventory2, contentDescription = "Stock") },
                                label = { Text("Inventory") },
                                modifier = Modifier.testTag("tab_inventory")
                            )
                            NavigationBarItem(
                                selected = currentRoute == Screen.Purchases.route,
                                onClick = { navController.navigate(Screen.Purchases.route) { popUpTo(navController.graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true } },
                                icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "Purchases") },
                                label = { Text("Purchases") },
                                modifier = Modifier.testTag("tab_purchases")
                            )
                            NavigationBarItem(
                                selected = currentRoute == Screen.Profile.route,
                                onClick = { navController.navigate(Screen.Profile.route) { popUpTo(navController.graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true } },
                                icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                                label = { Text("Profile") },
                                modifier = Modifier.testTag("tab_profile")
                            )
                        }
                        UserRole.KITCHEN_MASTER -> {
                            NavigationBarItem(
                                selected = currentRoute == Screen.KitchenDashboard.route,
                                onClick = { navController.navigate(Screen.KitchenDashboard.route) { popUpTo(navController.graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true } },
                                icon = { Icon(Icons.Default.SoupKitchen, contentDescription = "Kitchen") },
                                label = { Text("Kitchen") },
                                modifier = Modifier.testTag("tab_kitchen")
                            )
                            NavigationBarItem(
                                selected = currentRoute == Screen.ProductCatalog.route,
                                onClick = { navController.navigate(Screen.ProductCatalog.route) { popUpTo(navController.graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true } },
                                icon = { Icon(Icons.Default.Inventory2, contentDescription = "Ingredients") },
                                label = { Text("Ingredients") }
                            )
                            NavigationBarItem(
                                selected = currentRoute == Screen.Profile.route,
                                onClick = { navController.navigate(Screen.Profile.route) { popUpTo(navController.graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true } },
                                icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                                label = { Text("Profile") }
                            )
                        }
                        UserRole.DELIVERY_MAN -> {
                            NavigationBarItem(
                                selected = currentRoute == Screen.DeliveryDashboard.route,
                                onClick = { navController.navigate(Screen.DeliveryDashboard.route) { popUpTo(navController.graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true } },
                                icon = { Icon(Icons.Default.LocalShipping, contentDescription = "Deliveries") },
                                label = { Text("Deliveries") },
                                modifier = Modifier.testTag("tab_deliveries")
                            )
                            NavigationBarItem(
                                selected = currentRoute == Screen.Profile.route,
                                onClick = { navController.navigate(Screen.Profile.route) { popUpTo(navController.graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true } },
                                icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                                label = { Text("Profile") }
                            )
                        }
                        else -> {}
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = Screen.Splash.route,
                modifier = Modifier.padding(padding)
            ) {
            composable(Screen.Splash.route) {
                SplashScreen(
                    onSplashFinished = {
                        navController.navigate(Screen.RoleSelection.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.RoleSelection.route) {
                RoleSelectionScreen(
                    onRoleSelected = { role ->
                        navController.navigate(Screen.Login.createRoute(role))
                    },
                    onLoginClick = { role ->
                        navController.navigate(Screen.Login.createRoute(role))
                    }
                )
            }

            composable(Screen.Login.route) { backStackEntry ->
                val roleName = backStackEntry.arguments?.getString("role") ?: UserRole.ADMIN.name
                val role = try { UserRole.valueOf(roleName) } catch (e: Exception) { UserRole.ADMIN }

                LoginScreen(
                    initialRole = role,
                    onLoginSuccess = {
                        val targetRoute = when (role) {
                            UserRole.ADMIN -> Screen.AdminDashboard.route
                            UserRole.KITCHEN_MASTER -> Screen.KitchenDashboard.route
                            UserRole.DELIVERY_MAN -> Screen.DeliveryDashboard.route
                        }
                        navController.navigate(targetRoute) {
                            popUpTo(Screen.RoleSelection.route) { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() },
                    viewModel = authViewModel
                )
            }

            // Admin Routes
            composable(Screen.AdminDashboard.route) {
                AdminDashboardScreen(
                    repository = repository,
                    onNavigateToProducts = { navController.navigate(Screen.ProductCatalog.route) },
                    onNavigateToPurchases = { navController.navigate(Screen.Purchases.route) },
                    onNavigateToSuppliers = { navController.navigate(Screen.Suppliers.route) },
                    onNavigateToStaff = { navController.navigate(Screen.Staff.route) },
                    onNavigateToRecipes = { navController.navigate(Screen.Recipes.route) },
                    onNavigateToReports = { navController.navigate(Screen.Reports.route) },
                    onNavigateToWastage = { navController.navigate(Screen.Wastage.route) },
                    onNavigateToAnalytics = { navController.navigate(Screen.Analytics.route) },
                    onNavigateToForecast = { navController.navigate(Screen.InventoryForecast.route) },
                    onNavigateToFirebaseStatus = { navController.navigate(Screen.FirebaseStatus.route) },
                    onNavigateToProductDetails = { id -> navController.navigate(Screen.ProductDetails.createRoute(id)) }
                )
            }

            composable(Screen.FirebaseStatus.route) {
                FirebaseStatusScreen(
                    repository = repository,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.ProductCatalog.route) {
                ProductListScreen(
                    repository = repository,
                    userRole = currentUser?.role,
                    onAddProductClick = { navController.navigate(Screen.AddEditProduct.createRoute(null)) },
                    onProductClick = { id -> navController.navigate(Screen.ProductDetails.createRoute(id)) }
                )
            }

            composable(Screen.AddEditProduct.route) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("id")
                AddEditProductScreen(
                    repository = repository,
                    productIdToEdit = id,
                    onSaveSuccess = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.ProductDetails.route) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("id") ?: ""
                ProductDetailsScreen(
                    repository = repository,
                    productId = id,
                    userRole = currentUser?.role,
                    onBack = { navController.popBackStack() },
                    onEditClick = { editId -> navController.navigate(Screen.AddEditProduct.createRoute(editId)) }
                )
            }

            composable(Screen.Purchases.route) {
                PurchaseManagementScreen(
                    repository = repository,
                    onAddPurchaseClick = { navController.navigate(Screen.AddPurchase.route) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.AddPurchase.route) {
                AddPurchaseScreen(
                    repository = repository,
                    onSaveSuccess = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Suppliers.route) {
                SupplierManagementScreen(repository = repository, onBack = { navController.popBackStack() })
            }

            composable(Screen.Staff.route) {
                val context = androidx.compose.ui.platform.LocalContext.current
                val db = remember { com.example.data.local.BakeStockDatabase.getDatabase(context) }
                val empRepo = remember { com.example.data.repository.EmployeeRepository(db) }
                val empViewModel = remember { com.example.ui.admin.EmployeeManagementViewModel(empRepo) }
                EmployeeManagementScreen(viewModel = empViewModel, onBack = { navController.popBackStack() })
            }

            composable(Screen.Recipes.route) {
                RecipeManagementScreen(repository = repository, onBack = { navController.popBackStack() })
            }

            composable(Screen.Wastage.route) {
                WastageScreen(repository = repository, onBack = { navController.popBackStack() })
            }

            composable(Screen.Reports.route) {
                ReportsScreen(repository = repository, onBack = { navController.popBackStack() })
            }

            composable(Screen.Analytics.route) {
                AnalyticsDashboardScreen(repository = repository, onBack = { navController.popBackStack() })
            }

            composable(Screen.InventoryForecast.route) {
                InventoryForecastScreen(repository = repository, onBack = { navController.popBackStack() })
            }

            // Kitchen Routes
            composable(Screen.KitchenDashboard.route) {
                KitchenDashboardScreen(
                    repository = repository,
                    onNavigateToProductionPlan = { navController.navigate(Screen.ProductionPlan.route) },
                    onNavigateToIngredientRequest = { navController.navigate(Screen.KitchenRequest.route) },
                    onNavigateToBatchCompletion = { navController.navigate(Screen.CreateBatch.route) }
                )
            }

            composable(Screen.ProductionPlan.route) {
                ProductionPlanningScreen(
                    repository = repository,
                    onBack = { navController.popBackStack() },
                    onExecuteBatch = { navController.navigate(Screen.CreateBatch.route) }
                )
            }

            composable(Screen.CreateBatch.route) {
                CreateBatchScreen(
                    repository = repository,
                    onBack = { navController.popBackStack() },
                    onSuccess = { navController.popBackStack() }
                )
            }

            composable(Screen.KitchenRequest.route) {
                KitchenIngredientRequestScreen(
                    repository = repository,
                    onBack = { navController.popBackStack() },
                    onSuccess = { navController.popBackStack() }
                )
            }

            // Delivery Routes
            composable(Screen.DeliveryDashboard.route) {
                DeliveryDashboardScreen(
                    repository = repository,
                    onRequestClick = { id -> navController.navigate(Screen.DeliveryUpdate.createRoute(id)) }
                )
            }

            composable(Screen.DeliveryUpdate.route) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("id") ?: ""
                DeliveryUpdateScreen(
                    repository = repository,
                    requestId = id,
                    onBack = { navController.popBackStack() },
                    onSuccess = { navController.popBackStack() }
                )
            }

            // Shared Routes
            composable(Screen.Profile.route) {
                ProfileScreen(
                    authViewModel = authViewModel,
                    repository = repository,
                    onNavigateToNotifications = { navController.navigate(Screen.Notifications.route) },
                    onNavigateToActivityLogs = { navController.navigate(Screen.ActivityLogs.route) },
                    onLogout = {
                        navController.navigate(Screen.RoleSelection.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Notifications.route) {
                NotificationsScreen(repository = repository, onBack = { navController.popBackStack() })
            }

            composable(Screen.ActivityLogs.route) {
                ActivityLogsScreen(repository = repository, onBack = { navController.popBackStack() })
            }
        }

        if (!showBottomBar) {
            FilledTonalIconButton(
                onClick = { isWhiteBackground = !isWhiteBackground },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 8.dp, end = 12.dp)
                    .size(36.dp)
                    .testTag("toggle_white_bg_button_overlay"),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = if (isWhiteBackground) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Icon(
                    imageVector = if (isWhiteBackground) Icons.Default.Palette else Icons.Outlined.Palette,
                    contentDescription = "Switch Background Color",
                    modifier = Modifier.size(20.dp),
                    tint = if (isWhiteBackground) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        }
    }
}
}
