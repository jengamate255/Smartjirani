package com.smartjirani.feature.civic.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartjirani.core.designsystem.component.ErrorBanner
import com.smartjirani.core.designsystem.component.SmartEmptyState
import com.smartjirani.core.designsystem.component.SmartLoading
import com.smartjirani.core.designsystem.component.SmartScaffold
import com.smartjirani.feature.civic.CivicAdminUnit
import com.smartjirani.feature.civic.CivicDirectoryUser
import com.smartjirani.feature.civic.CivicRole
import com.smartjirani.feature.civic.CivicSlaRule
import com.smartjirani.feature.civic.R
import kotlinx.coroutines.delay

private val AssignableRoles =
    listOf(
        CivicRole.ROLE_MJUMBE to R.string.civic_role_mjumbe,
        CivicRole.ROLE_MWENYEKITI to R.string.civic_role_mwenyekiti,
        CivicRole.ROLE_MTAA_OFFICER to R.string.civic_role_mtaa_officer,
        CivicRole.ROLE_WARD_OFFICER to R.string.civic_role_ward_officer,
        CivicRole.ROLE_MUNICIPAL_OFFICER to R.string.civic_role_municipal_officer,
        CivicRole.ROLE_DEPARTMENT_OFFICER to R.string.civic_role_department_officer,
        CivicRole.ROLE_COUNCIL_ADMIN to R.string.civic_role_council_admin,
        CivicRole.ROLE_SYSTEM_ADMIN to R.string.civic_role_system_admin,
    )

private fun roleLabelRes(role: String): Int = AssignableRoles.firstOrNull { it.first == role }?.second ?: R.string.civic_admin_title

@Composable
fun CivicAdminRoute(
    onNavigateBack: () -> Unit,
    viewModel: CivicAdminViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.toast) {
        if (uiState.toast != null) {
            delay(2_500)
            viewModel.dismissToast()
        }
    }

    CivicAdminScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onRefresh = viewModel::refresh,
        onDismissError = viewModel::dismissError,
        onSearchDirectory = viewModel::searchDirectory,
        onCreateUnit = viewModel::createUnit,
        onCreateCategory = viewModel::createCategory,
        onCreateSlaRule = viewModel::createSlaRule,
        onAssignRole = viewModel::assignRole,
        onRevokeRole = viewModel::revokeRole,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CivicAdminScreen(
    uiState: CivicAdminUiState,
    onNavigateBack: () -> Unit,
    onRefresh: () -> Unit,
    onDismissError: () -> Unit,
    onSearchDirectory: (String, (List<CivicDirectoryUser>) -> Unit) -> Unit,
    onCreateUnit: (String, String, String?, String?) -> Unit,
    onCreateCategory: (String, String, String?, Boolean) -> Unit,
    onCreateSlaRule: (String?, String?, String?, Double, Double) -> Unit,
    onAssignRole: (String, String, String) -> Unit,
    onRevokeRole: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showUnitDialog by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showSlaDialog by remember { mutableStateOf(false) }
    var showAssignDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.civic_admin_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.civic_back),
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            when {
                uiState.isLoading -> {
                    SmartLoading()
                }

                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        uiState.error?.let { message ->
                            ErrorBanner(
                                message = message,
                                onDismiss = onDismissError,
                                onRetry = onRefresh,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            item(key = "roles-header") {
                                SectionHeader(
                                    title = stringResource(R.string.civic_admin_section_roles),
                                    actionLabel = stringResource(R.string.civic_admin_assign),
                                    onAction = { showAssignDialog = true },
                                )
                            }
                            if (uiState.data.roles.isEmpty()) {
                                item(key = "roles-empty") {
                                    EmptyHint(stringResource(R.string.civic_admin_roles_empty))
                                }
                            } else {
                                items(uiState.data.roles, key = { "role-${it.id}" }) { row ->
                                    RoleRowCard(
                                        row = row,
                                        onRevoke = { onRevokeRole(row.id) },
                                        enabled = !uiState.isMutating,
                                    )
                                }
                            }

                            item(key = "units-header") {
                                SectionHeader(
                                    title = stringResource(R.string.civic_admin_section_units),
                                    actionLabel = stringResource(R.string.civic_admin_unit_add),
                                    onAction = { showUnitDialog = true },
                                )
                            }
                            if (uiState.data.units.isEmpty()) {
                                item(key = "units-empty") {
                                    EmptyHint(stringResource(R.string.civic_admin_units_empty))
                                }
                            } else {
                                items(uiState.data.units, key = { "unit-${it.id}" }) { unit ->
                                    UnitCard(unit)
                                }
                            }

                            item(key = "categories-header") {
                                SectionHeader(
                                    title = stringResource(R.string.civic_admin_section_categories),
                                    actionLabel = stringResource(R.string.civic_admin_category_add),
                                    onAction = { showCategoryDialog = true },
                                )
                            }
                            if (uiState.data.categories.isEmpty()) {
                                item(key = "categories-empty") {
                                    EmptyHint(stringResource(R.string.civic_admin_categories_empty))
                                }
                            } else {
                                items(uiState.data.categories, key = { "cat-${it.id}" }) { category ->
                                    CategoryCard(category)
                                }
                            }

                            item(key = "sla-header") {
                                SectionHeader(
                                    title = stringResource(R.string.civic_admin_section_sla),
                                    actionLabel = stringResource(R.string.civic_admin_sla_add),
                                    onAction = { showSlaDialog = true },
                                )
                            }
                            if (uiState.data.slaRules.isEmpty()) {
                                item(key = "sla-empty") {
                                    EmptyHint(stringResource(R.string.civic_admin_sla_empty))
                                }
                            } else {
                                items(uiState.data.slaRules, key = { "sla-${it.id}" }) { rule ->
                                    SlaRuleCard(rule)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAssignDialog) {
        AssignRoleDialog(
            units = uiState.data.units,
            isMutating = uiState.isMutating,
            onSearch = onSearchDirectory,
            onAssign = { userId, role, unitId ->
                showAssignDialog = false
                onAssignRole(userId, role, unitId)
            },
            onDismiss = { showAssignDialog = false },
        )
    }
    if (showUnitDialog) {
        CreateUnitDialog(
            unitTypes = uiState.data.unitTypes,
            units = uiState.data.units,
            isMutating = uiState.isMutating,
            onCreate = { typeId, name, parentId, code ->
                showUnitDialog = false
                onCreateUnit(typeId, name, parentId, code)
            },
            onDismiss = { showUnitDialog = false },
        )
    }
    if (showCategoryDialog) {
        CreateCategoryDialog(
            isMutating = uiState.isMutating,
            onCreate = { nameEn, nameSw, description, requiresVerification ->
                showCategoryDialog = false
                onCreateCategory(nameEn, nameSw, description, requiresVerification)
            },
            onDismiss = { showCategoryDialog = false },
        )
    }
    if (showSlaDialog) {
        CreateSlaDialog(
            categories = uiState.data.categories,
            isMutating = uiState.isMutating,
            onCreate = { categoryId, severity, priority, response, resolution ->
                showSlaDialog = false
                onCreateSlaRule(categoryId, severity, priority, response, resolution)
            },
            onDismiss = { showSlaDialog = false },
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onAction) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(actionLabel)
        }
    }
}

@Composable
private fun EmptyHint(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp),
    )
}

@Composable
private fun RoleRowCard(
    row: com.smartjirani.feature.civic.CivicRoleRow,
    onRevoke: () -> Unit,
    enabled: Boolean,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.PersonAdd,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = row.displayName ?: row.email ?: row.userId.take(8),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(roleLabelRes(row.role)) + " · " + row.unitName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text =
                    stringResource(
                        if (row.isActive) {
                            R.string.civic_admin_active
                        } else {
                            R.string.civic_admin_inactive
                        },
                    ),
                style = MaterialTheme.typography.labelSmall,
                color =
                    if (row.isActive) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
            )
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(onClick = onRevoke, enabled = enabled) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.civic_admin_revoke),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun UnitCard(unit: CivicAdminUnit) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = unit.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = unit.unitType,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (unit.code != null) {
                Text(
                    text = unit.code,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CategoryCard(category: com.smartjirani.feature.civic.CivicAdminCategory) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = category.nameSw,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = category.nameEn,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SlaRuleCard(rule: CivicSlaRule) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            val scope = listOfNotNull(rule.categorySw, rule.severity, rule.priority)
            Text(
                text =
                    if (scope.isEmpty()) {
                        stringResource(R.string.civic_admin_sla_scope_all)
                    } else {
                        scope.joinToString(" · ")
                    },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text =
                    stringResource(R.string.civic_admin_sla_response_hint) + ": " +
                        rule.responseHours
                            .toBigDecimal()
                            .stripTrailingZeros()
                            .toPlainString() + " · " +
                        stringResource(R.string.civic_admin_sla_resolution_hint) + ": " +
                        rule.resolutionHours
                            .toBigDecimal()
                            .stripTrailingZeros()
                            .toPlainString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AssignRoleDialog(
    units: List<CivicAdminUnit>,
    isMutating: Boolean,
    onSearch: (String, (List<CivicDirectoryUser>) -> Unit) -> Unit,
    onAssign: (String, String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<CivicDirectoryUser>>(emptyList()) }
    var selectedUser by remember { mutableStateOf<CivicDirectoryUser?>(null) }
    var role by remember { mutableStateOf(CivicRole.ROLE_MTAA_OFFICER) }
    var unitId by remember { mutableStateOf<String?>(null) }
    var searched by remember { mutableStateOf(false) }

    LaunchedEffect(query) {
        val term = query.trim()
        if (term.length >= 2) {
            delay(300)
            onSearch(term) { found ->
                results = found
                searched = true
            }
        } else {
            results = emptyList()
            searched = false
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isMutating) onDismiss() },
        title = { Text(stringResource(R.string.civic_admin_assign_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (selectedUser == null) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text(stringResource(R.string.civic_admin_assign_search_hint)) },
                        singleLine = true,
                        enabled = !isMutating,
                    )
                    if (searched && results.isEmpty()) {
                        Text(
                            text = stringResource(R.string.civic_admin_assign_empty),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    results.forEach { user ->
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors =
                                CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                ),
                            onClick = { selectedUser = user },
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = user.displayName ?: user.username ?: user.id.take(8),
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                user.email?.let {
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        text = selectedUser?.displayName ?: selectedUser?.email.orEmpty(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Text(
                    text = stringResource(R.string.civic_admin_assign_role_label),
                    style = MaterialTheme.typography.labelMedium,
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    AssignableRoles.forEach { (key, labelRes) ->
                        FilterChip(
                            selected = role == key,
                            onClick = { role = key },
                            enabled = !isMutating,
                            label = { Text(stringResource(labelRes)) },
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.civic_admin_assign_unit_label),
                    style = MaterialTheme.typography.labelMedium,
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    units.forEach { unit ->
                        FilterChip(
                            selected = unitId == unit.id,
                            onClick = { unitId = unit.id },
                            enabled = !isMutating,
                            label = { Text(unit.name) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val user = selectedUser
                    val unit = unitId
                    if (user != null && unit != null) onAssign(user.id, role, unit)
                },
                enabled = !isMutating && selectedUser != null && unitId != null,
            ) {
                Text(stringResource(R.string.civic_admin_assign_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isMutating) {
                Text(stringResource(R.string.civic_common_cancel))
            }
        },
    )
}

@Composable
private fun CreateUnitDialog(
    unitTypes: List<com.smartjirani.feature.civic.CivicAdminUnitType>,
    units: List<CivicAdminUnit>,
    isMutating: Boolean,
    onCreate: (String, String, String?, String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var typeId by remember { mutableStateOf<String?>(null) }
    var name by remember { mutableStateOf("") }
    var parentId by remember { mutableStateOf<String?>(null) }
    var code by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!isMutating) onDismiss() },
        title = { Text(stringResource(R.string.civic_admin_unit_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.civic_admin_unit_type_label),
                    style = MaterialTheme.typography.labelMedium,
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    unitTypes.forEach { type ->
                        FilterChip(
                            selected = typeId == type.id,
                            onClick = { typeId = type.id },
                            enabled = !isMutating,
                            label = { Text(type.name) },
                        )
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 120) name = it },
                    label = { Text(stringResource(R.string.civic_admin_unit_name_hint)) },
                    singleLine = true,
                    enabled = !isMutating,
                )
                Text(
                    text = stringResource(R.string.civic_admin_unit_parent_label),
                    style = MaterialTheme.typography.labelMedium,
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    FilterChip(
                        selected = parentId == null,
                        onClick = { parentId = null },
                        enabled = !isMutating,
                        label = { Text(stringResource(R.string.civic_admin_unit_parent_none)) },
                    )
                    units.forEach { unit ->
                        FilterChip(
                            selected = parentId == unit.id,
                            onClick = { parentId = unit.id },
                            enabled = !isMutating,
                            label = { Text(unit.name) },
                        )
                    }
                }
                OutlinedTextField(
                    value = code,
                    onValueChange = { if (it.length <= 30) code = it },
                    label = { Text(stringResource(R.string.civic_admin_unit_code_hint)) },
                    singleLine = true,
                    enabled = !isMutating,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val type = typeId ?: return@TextButton
                    onCreate(type, name.trim(), parentId, code.trim().takeIf { it.isNotBlank() })
                },
                enabled = !isMutating && typeId != null && name.trim().length >= 2,
            ) {
                Text(stringResource(R.string.civic_common_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isMutating) {
                Text(stringResource(R.string.civic_common_cancel))
            }
        },
    )
}

@Composable
private fun CreateCategoryDialog(
    isMutating: Boolean,
    onCreate: (String, String, String?, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var nameEn by remember { mutableStateOf("") }
    var nameSw by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var requiresVerification by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = { if (!isMutating) onDismiss() },
        title = { Text(stringResource(R.string.civic_admin_category_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = nameEn,
                    onValueChange = { if (it.length <= 80) nameEn = it },
                    label = { Text(stringResource(R.string.civic_admin_category_name_en_hint)) },
                    singleLine = true,
                    enabled = !isMutating,
                )
                OutlinedTextField(
                    value = nameSw,
                    onValueChange = { if (it.length <= 80) nameSw = it },
                    label = { Text(stringResource(R.string.civic_admin_category_name_sw_hint)) },
                    singleLine = true,
                    enabled = !isMutating,
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { if (it.length <= 300) description = it },
                    label = { Text(stringResource(R.string.civic_admin_category_desc_hint)) },
                    minLines = 2,
                    maxLines = 4,
                    enabled = !isMutating,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = requiresVerification,
                        onCheckedChange = { requiresVerification = it },
                        enabled = !isMutating,
                    )
                    Text(
                        text = stringResource(R.string.civic_admin_category_verify),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onCreate(
                        nameEn.trim(),
                        nameSw.trim(),
                        description.trim().takeIf { it.isNotBlank() },
                        requiresVerification,
                    )
                },
                enabled = !isMutating && nameEn.trim().length >= 2 && nameSw.trim().length >= 2,
            ) {
                Text(stringResource(R.string.civic_common_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isMutating) {
                Text(stringResource(R.string.civic_common_cancel))
            }
        },
    )
}

@Composable
private fun CreateSlaDialog(
    categories: List<com.smartjirani.feature.civic.CivicAdminCategory>,
    isMutating: Boolean,
    onCreate: (String?, String?, String?, Double, Double) -> Unit,
    onDismiss: () -> Unit,
) {
    var categoryId by remember { mutableStateOf<String?>(null) }
    var severity by remember { mutableStateOf<String?>(null) }
    var priority by remember { mutableStateOf<String?>(null) }
    var responseHours by remember { mutableStateOf("24") }
    var resolutionHours by remember { mutableStateOf("72") }

    val response = responseHours.toDoubleOrNull()
    val resolution = resolutionHours.toDoubleOrNull()

    AlertDialog(
        onDismissRequest = { if (!isMutating) onDismiss() },
        title = { Text(stringResource(R.string.civic_admin_sla_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.civic_admin_sla_category_label),
                    style = MaterialTheme.typography.labelMedium,
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    FilterChip(
                        selected = categoryId == null,
                        onClick = { categoryId = null },
                        enabled = !isMutating,
                        label = { Text(stringResource(R.string.civic_admin_sla_scope_all)) },
                    )
                    categories.forEach { category ->
                        FilterChip(
                            selected = categoryId == category.id,
                            onClick = { categoryId = category.id },
                            enabled = !isMutating,
                            label = { Text(category.nameSw) },
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.civic_admin_sla_severity_label),
                    style = MaterialTheme.typography.labelMedium,
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    listOf("LOW" to R.string.civic_severity_low, "MEDIUM" to R.string.civic_severity_medium, "HIGH" to R.string.civic_severity_high, "CRITICAL" to R.string.civic_severity_critical).forEach { (key, labelRes) ->
                        FilterChip(
                            selected = severity == key,
                            onClick = { severity = if (severity == key) null else key },
                            enabled = !isMutating,
                            label = { Text(stringResource(labelRes)) },
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.civic_admin_sla_priority_label),
                    style = MaterialTheme.typography.labelMedium,
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    listOf("LOW" to R.string.civic_priority_low, "NORMAL" to R.string.civic_priority_normal, "HIGH" to R.string.civic_priority_high, "URGENT" to R.string.civic_priority_urgent).forEach { (key, labelRes) ->
                        FilterChip(
                            selected = priority == key,
                            onClick = { priority = if (priority == key) null else key },
                            enabled = !isMutating,
                            label = { Text(stringResource(labelRes)) },
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = responseHours,
                        onValueChange = { responseHours = it.filter { ch -> ch.isDigit() || ch == '.' }.take(5) },
                        label = { Text(stringResource(R.string.civic_admin_sla_response_hint)) },
                        singleLine = true,
                        enabled = !isMutating,
                        keyboardOptions =
                            androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                            ),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = resolutionHours,
                        onValueChange = { resolutionHours = it.filter { ch -> ch.isDigit() || ch == '.' }.take(5) },
                        label = { Text(stringResource(R.string.civic_admin_sla_resolution_hint)) },
                        singleLine = true,
                        enabled = !isMutating,
                        keyboardOptions =
                            androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                            ),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onCreate(categoryId, severity, priority, response ?: 0.0, resolution ?: 0.0)
                },
                enabled =
                    !isMutating && response != null && response > 0 &&
                        resolution != null && resolution > 0,
            ) {
                Text(stringResource(R.string.civic_common_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isMutating) {
                Text(stringResource(R.string.civic_common_cancel))
            }
        },
    )
}
