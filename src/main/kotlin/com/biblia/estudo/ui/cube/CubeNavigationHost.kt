package com.biblia.estudo.ui.cube

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollOrigin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.offset
import androidx.compose.ui.draw.zoom
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.currentBackStackEntry
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.isSemanticallyClickable
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.systems.size.Memory
import androidx.compose.ui.text.text
import androidx.compose.ui.text.style.TextStyle
import androidx.compose.util.remember

import com.biblia.estudo.R
import com.biblia.estudo.ui.search.SearchActivity
import com.biblia.estudo.ui.library.HomeActivity
import com.biblia.estudo.ui.bible.BibleReaderActivity
import com.biblia.estudo.ui.highlights.HighlightsActivity
import com.biblia.estudo.ui.favorites.FavoritesActivity
import com.biblia.estudo.ui.notes.NotesActivity
import com.biblia.estudo.ui.readingplan.ReadingPlanActivity
import com.biblia.estudo.ui.settings.SettingsActivity
import com.biblia.estudo.ui.about.AboutActivity
import com.biblia.estudo.ui.resources.ResourcesActivity
import com.biblia.estudo.ui.dictionary.DictionaryActivity
import com.biblia.estudo.ui.maps.MapsActivity
import com.biblia.estudo.ui.timeline.TimelineActivity
import com.biblia.estudo.ui.calendar.CalendarActivity // Assuming exists
import com.biblia.estudo.ui.calendar.CalendarActivity
import com.biblia.estudo.ui.cube.Screen

import androidx.compose.navigation.core.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.biblia.estudo.ui.cube.Scope
import com.biblia.estudo.ui.cube.viewModel
import com.biblia.estudo.ui.cube.viewModels
import com.biblia.estudo.utils.NavigationHelper
import kotlin.collections.ArrayList
import kotlin.properties.ReadWrite

// Data class para definir cada tela do cubo
data class Screen(
    val id: Int,
    val composable: @Composable (() -> Unit),
    val title: String,
    val iconRes: Int,
    val screenClass: String // Nome da Activity correspondente
)

// ViewModel para gerenciar o estado da navegaçãoCube
@ViewModelScoped
class CubeViewModel : ViewModel() {
    // Estado da página atualmente visível
    var currentPage by mutableStateOf(0)
        private set
    
    // Lista de telas do cubo
    val screens: List<Screen> = remember {
        listOf(
            Screen(
                id = 0,
                composable = { HomeScreen() },
                title = "Início",
                iconRes = R.drawable.ic_home_black_24dp,
                screenClass = "com.biblia.estudo.ui.library.HomeActivity"
            ),
            Screen(
                id = 1,
                composable = { BibleReaderScreen() },
                title = "Bíblia",
                iconRes = R.drawable.ic_library_book_black_24dp,
                screenClass = "com.biblia.estudo.ui.bible.BibleReaderActivity"
            ),
            Screen(
                id = 2,
                composable = { SearchScreen() },
                title = "Pesquisar",
                iconRes = R.drawable.ic_search_black_24dp,
                screenClass = "com.biblia.estudo.ui.search.SearchActivity"
            ),
            Screen(
                id = 3,
                composable = { HighlightsScreen() },
                title = "Destaques",
                iconRes = R.drawable.ic_favorite_black_24dp,
                screenClass = "com.biblia.estudo.ui.highlights.HighlightsActivity"
            ),
            Screen(
                id = 4,
                composable = { FavoritesScreen() },
                title = "Favoritos",
                iconRes = R.drawable.ic_favorite_border_black_24dp,
                screenClass = "com.biblia.estudo.ui.favorites.FavoritesActivity"
            ),
            Screen(
                id = 5,
                composable = { NotesScreen() },
                title = "Notas",
                iconRes = R.drawable.ic_note_black_24dp,
                screenClass = "com.biblia.estudo.ui.notes.NotesActivity"
            )
        )
    }
    
    // Método para mudar página com animação cube
    fun setPage(page: Int) {
        if (page in 0 until screens.size) {
            currentPage = page
        }
    }
    
    // Método para obter título da página atual
    fun getCurrentTitle(): String = screens[currentPage].title
    
    // Método para obter ícone da página atual
    fun getCurrentIconRes(): Int = screens[currentPage].iconRes
}

// Composable principal que injeta o ViewModel e exibe o Cube3DPager
@Composable
fun CubeNavigationHost() {
    // Injetar ViewModel
    val viewModel: CubeViewModel = hiltViewModel() // ou viewModel()
    
    // State para bottom navigation synchronization
    val selectedPage by viewModel.currentPage.collectAsState()
    
    // Converter screens do viewModel para composable
    val scaffoldContent: @Composable (selectedPage: Int) -> Unit = { selected ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(.never),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            // O Cube3DPager ocupa a área principal
            Cube3DPager(
                screenCount = viewModel.screens.size,
                onPageChanged = { newPage ->
                    viewModel.setPage(newPage)
                    // Atualizar título da toolbar, se existir
                    // Toast.makeText(context, "Página $newPage", Toast.LENGTH_SHORT).show()
                },
                screenContent = { pageIndex ->
                    // Exibir a screen correspondente pelo índice
                    val screen = viewModel.screens[pageIndex]
                    
                    // Navegação específica baseada no tipo de tela
                    when (screen.id) {
                        0 -> HomeScreen()
                        1 -> BibleReaderScreen()
                        2 -> SearchScreen()
                        3 -> HighlightsScreen()
                        4 -> FavoritesScreen()
                        5 -> NotesScreen()
                        else -> HomeScreen()
                    }
                }
            )
        }
    }
    
    // Bottom Navigation synchronizada com o Cube
    BibleBottomNav(
        selectedIndex = selectedPage,
        onItemClick = { page ->
            viewModel.setPage(page)
            // Scroll do Cube3DPager para página específica
            // pagerState?.currentPage = page
        }
    )
}

// Telas individuais - cada uma ocupa um "face" do cubo

@Composable
fun HomeScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterStart
    ) {
        Text(
            text = "📚 Bem-vindo ao Estudo Bíblico",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(16.dp)
        )
        
        // Conteúdo principal da home
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            elevation = 4.dp,
            shadowColor = MaterialTheme.colorScheme.onSurface
        ) {
            Column {
                Text(
                    text = "Seu ponto de partida para o estudo da Bíblia",
                    style = MaterialTheme.typography.body1,
                    textAlign = TextAlign.Center
                )
                // Botões de atalho ou destaques recentes
            }
        }
    }
}

@Composable
fun BibleReaderScreen() {
    // Tela de leitura bíblica - existente no projeto
    // Apenas envolvida no layout do cubo
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterStart
    ) {
        Text(
            text = "Tela de Leitura da Bíblia",
            style = MaterialTheme.typography.headline6,
            modifier = Modifier.padding(24.dp)
        )
        // Aqui iria o conteúdo real do BibleReaderActivity adaptado para Compose
    }
}

@Composable
fun SearchScreen() {
    // Tela de pesquisa - adaptada do SearchActivity
    // Campo de busca e resultados
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center
    ) {
        TextField(
            value = "",
            onValueChange = { /* */ },
            placeholder = "Digite um versículo ou termo...",
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        )
        
        Text(
            text: "Digite um termo de busca acima",
            style = MaterialTheme.typography.body2
        )
    }
}

@Composable
fun HighlightsScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterStart
    ) {
        Text(
            text = "Minhas Destaques",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(16.dp)
        )
        // Lista de destaques do usuário
        Text("Suas notas e destaques aparecerão aqui", style = MaterialTheme.typography.body1)
    }
}

@Composable
fun FavoritesScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterStart
    ) {
        Text(
            text = "⭐ Favoritos",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(16.dp)
        )
        Text("Seus versículos favoritos aparecerão aqui", style = MaterialTheme.typography.body1)
    }
}

@Composable
fun NotesScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterStart
    ) {
        Text(
            text = "📝 Minhas Notas",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(16.dp)
        )
        Text("Suas anotações de estudo aparecerão aqui", style = MaterialTheme.typography.body1)
    }
}

// Bottom Navigation synchronizada com o efeito Cube
@Composable
fun BibleBottomNav(
    selectedIndex: Int,
    onItemClick: (Int) -> Unit
) {
    val context = LocalContext.current
    
    // Configuração dos 6 ícones da navegação bottom
    val navItems = listOf(
        NavItem(iconRes = R.drawable.ic_home_black_24dp, label = "Início", id = 0),
        NavItem(iconRes = R.drawable.ic_library_book_black_24dp, label = "Bíblia", id = 1),
        NavItem(iconRes = R.drawable.ic_search_black_24dp, label = "Pesquisar", id = 2),
        NavItem(iconRes = R.drawable.ic_favorite_black_24dp, label = "Destaques", id = 3),
        NavItem(iconRes = R.drawable.ic_favorite_border_black_24dp, label = "Favoritos", id = 4),
        NavItem(iconRes = R.drawable.ic_note_black_24dp, label = "Notas", id = 5)
    )
    
    // Filtrar o item selecionado
    val currentNavItem = navItems.find { it.id == selectedIndex }!!
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(MaterialTheme.colorScheme.surface),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.EqualSpacing
    ) {
        navItems.forEachIndexed { index, navItem ->
            val isSelected = selectedIndex == index
            
            // Efito de seleção visual
            val iconTint = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
            
            // Texto colorido baseado na seleção
            val textColor = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
            
            Button(
                onClick = { onItemClick(index) },
                modifier = Modifier.weight(1f),
                enabled = true,
                defaultActivatedState = ButtonActivatedState.Default,
                pressedIndication = null,
                focusIndication = null,
                clickIndicator = null
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .gravity(Alignment.Center),
                        contentDescription = navItem.label,
                        // Ícone central
                        Icon(
                            imageVector = when (index) {
                                0 -> Icons.Filled.Home
                                1 -> Icons.Filled.Book
                                2 -> Icons.Filled.Search
                                3 -> Icons.Filled.Star
                                4 -> Icons.Filled.HeartOutline
                                5 -> Icons.Filled.Comment
                                else -> Icons.Filled.Help
                            },
                            tint = iconTint,
                            contentDescription = navItem.label
                        )
                    )
                
                // Label abaixo do ícone
                Text(
                    text = navItem.label,
                    style = smallLabelStyle,
                    tint = textColor,
                    modifier = Modifier.top(4.dp)
                )
            }
        )
    }
    
    // Helper composables para consistência
    val smallLabelStyle = TextStyle(
        fontSize = 10.sp,
        fontWeight = FontWeight.Normal
    )
}

// Modelos de dados auxiliares
data class NavItem(
    val iconRes: Int,
    val label: String,
    val id: Int
)