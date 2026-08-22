package com.biblia.estudo.ui.cube

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout. Modifier
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.camera.Camera
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics ProjectionMode
import androidx.compose.ui.graphics.transform.*
import androidx.compose.ui.layout.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.rememberPixelSize
import androidx.compose.ui.unit.size
import androidx.compose.ui.unit.width
import androidx.compose.ui.unit.height
import androidx.compose.ui.util.connsumers.ContentDrawModifier
import androidx.compose.ui.util.collection.SparseArray
import androidx.compose.ui.util.shift
import androidx.compose.ui.viewinterceptor.ViewInterview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.ui.HorizontalPager
import androidx.paging.ui.Page
import androidx.paging.ui.
import androidx.paging.ui.HorizontalPagerState
import androidx.paging.ui rememberHorizontalPagerState
import androidx.paging.ui.pager
import com.biblia.estudo.R
import com.biblia.estudo.ui.search.SearchActivity
import com.biblia.estudo.ui.library.HomeActivity
import com.biblia.estudo.ui.bible.BibleReaderActivity
import kotlin.Double
import kotlin.math.abs
import kotlin.math.coerceIn
import kotlin.math.max
import kotlin.math.min

@OptIn(multiplatform = KotlinExperimentalAPI::class)
@Composable
fun Cube3DPager(
    screenCount: Int,
    onPageChanged: (Int) -> Unit,
    screenContent: @Composable (pageIndex: Int) -> Unit,
    reverseSwiping: Boolean = true
) {
    // Estado do paginador horizontal
    val pagerState = rememberHorizontalPagerState(count = screenCount)
    val context = LocalContext.current
    
    // Configurações 3D otimizadas para efeito "Desktop Cube"
    val cameraDistance: Dp = 720.dp  // Distância da câmera - "ponto doce" para efeito cubo
    val rotationRange: Float = 90f   // Ângulo máximo de rotação ±90°
    val animationSpec = remember { spring(dampingRatio = 0.6f, stiffness = 120f) }
    
    HorizontalPager(
        state = pagerState,
        count = screenCount,
        indicator = null,
        modifier = Modifier
            .graphicsLayer(
                projectionMode = ProjectionMode.Perspective, // Perspectiva para profundidade real
                onPlaced { camera ->
                    // Camera configurada após layout - pode ser usada para efeitos avançados
                }
            )
            .fillMaxSize()
    ) { index ->
        // Conteúdo de cada tela envolvido em Box para aplicar efeitos 3D
        Box(
            modifier = Modifier
                .graphicsLayer(
                    // --- LÓGICA DE ROTAÇÃO 3D ---
                    // calculation baseada no offset da página atual
                    rotationY = remember { calculateRotationY(pagerState, index, rotationRange) },
                    // Distância da câmera cria profundidade perspectiva
                    cameraDistance = cameraDistance,
                    // Pivot no centro da tela para rotação equilibrada
                    pivotX = { it.width / 2f },
                    pivotY = { it.height / 2f },
                    // Modo perspectiva cria efeito 3D real (não achatado)
                    projectionMode = ProjectionMode.Perspective
                )
            // Background superficial para dar sensação de profundidade
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.small
            )
            // Sombra leve quando a tela está centralizada
            .then(
                if (index == pagerState.currentPage) {
                    Modifier.shadow(
                        shape = MaterialTheme.shapes.small,
                        elevation = 8.dp,
                        shadowColor = MaterialTheme.colorScheme.shadow
                    )
                } else {
                    Modifier
                }
            )
        ) {
            // Conteúdo real da tela - passado como parâmetro
            screenContent(index)
        }
    }
    
    // Listener para mudança de página - atualiza seleção na nav bottom
    pagerState.addOnPageChangeListener { oldIndex, newIndex, offset ->
        onPageChanged(newIndex)
        
        // Efeito adicional: ajustar opacidade das páginas adjacentes
        val adjacentAlpha = 1f - Math.abs(offset) * 0.3f
        // Este valor poderia ser usado para modificar a opacidade visual
    }
}

private fun calculateRotationY(
    pagerState: HorizontalPagerState, 
    currentIndex: Int, 
    range: Float
): Float {
    // Pega o offset atual do swipe (-1 a +1)
    val offset = pagerState.pageLayoutInfo?.offset ?: 0f
    
    // Determina direção do swipe
    val isSwipingLeft = offset < 0
    
    // Normaliza o offset no intervalo -1 a +1
    val normalizedOffset = offset.coerceIn(-1f, 1f)
    
    // Calcula ângulo bruto: -90° a +90° baseado no offset
    val rawAngle = normalizedOffset * range
    
    // Lógica do efeito "cubo":
    // - Quando o usuário desliza, a página atual gira para o lado
    // - A próxima página entra da direção oposta
    return when {
        // Tela central (índice atual) permanece plana (0°)
        currentIndex == pagerState.currentPage -> 0f
        
        // Se estiver deslizando para a esquerda e a página alvo está à esquerda
        isSwipingLeft && currentIndex == pagerState.currentPage - 1 -> rawAngle
        
        // Se estiver deslizando para a direita e a página alvo está à direita
        !isSwipingLeft && currentIndex == pagerState.currentPage + 1 -> -rawAngle
        
        // Casos extremos/ânimos: manter tela plana
        else -> 0f
    }.coerceIn(-range, range)
}