package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.BandMember
import com.example.data.GameDefinitions
import com.example.data.GuitarLevel
import com.example.data.SongRecord
import com.example.data.Venue
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val points by viewModel.points.collectAsStateWithLifecycle()
    val totalClicks by viewModel.totalClicks.collectAsStateWithLifecycle()
    val guitarLevel by viewModel.guitarLevel.collectAsStateWithLifecycle()
    val venueLevels by viewModel.venueLevels.collectAsStateWithLifecycle()
    val songLevels by viewModel.songLevels.collectAsStateWithLifecycle()
    val peopleLevels by viewModel.peopleLevels.collectAsStateWithLifecycle()
    val unlockedAchievements by viewModel.unlockedAchievements.collectAsStateWithLifecycle()
    val clickIndicators by viewModel.clickIndicators.collectAsStateWithLifecycle()
    val offlineEarned by viewModel.offlineEarned.collectAsStateWithLifecycle()
    val offlineSeconds by viewModel.offlineSeconds.collectAsStateWithLifecycle()
    val isLoaded by viewModel.isLoaded.collectAsStateWithLifecycle()

    val pps = viewModel.calculatePassiveIncome()
    val clickPower = viewModel.calculateClickPower()

    var activeTab by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    if (!isLoaded) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(RockBlack),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = RockYellow)
        }
        return
    }

    // Показ диалога оффлайн дохода
    offlineEarned?.let { earned ->
        OfflineEarningsDialog(
            earned = earned,
            seconds = offlineSeconds,
            onDismiss = { viewModel.dismissOfflineDialog() }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = RockBlack
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. Шапка со статистикой и балансом
            HeaderSection(
                points = points,
                pps = pps,
                clickPower = clickPower,
                totalClicks = totalClicks,
                unlockedAchievementsCount = unlockedAchievements.size
            )

            // 2. Интерактивная зона кликера (Цой в центре)
            Box(
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                ClickZone(
                    onTsoiClicked = { x, y -> viewModel.onTsoiClicked(x, y) },
                    clickIndicators = clickIndicators,
                    pps = pps
                )
            }

            // Разделительная линия в рок-стиле (красная полоса)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(RockRed)
            )

            // 3. Зона магазина / прогресса (Нижняя половина)
            Column(
                modifier = Modifier
                    .weight(1.3f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                RockDarkGrey,
                                Color(0xFF0C0C0F)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.12f),
                                Color.Transparent
                            )
                        ),
                        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                    )
            ) {
                // Вкладки магазина
                TabRow(
                    selectedTabIndex = activeTab,
                    containerColor = Color.Transparent,
                    contentColor = RockYellow,
                    indicator = { tabPositions ->
                        if (activeTab < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                                color = RockYellow
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.02f))
                ) {
                    val tabs = listOf("Гитары & Группа", "Запись Песен", "Концерты", "Достижения")
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = activeTab == index,
                            onClick = { activeTab = index },
                            text = {
                                Text(
                                    text = title,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.SansSerif,
                                    color = if (activeTab == index) RockYellow else RockGrey,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        )
                    }
                }

                // Содержимое активной вкладки
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                ) {
                    when (activeTab) {
                        0 -> GuitarsAndPeopleTab(
                            viewModel = viewModel,
                            points = points,
                            guitarLevel = guitarLevel,
                            peopleLevels = peopleLevels
                        )
                        1 -> SongsTab(
                            viewModel = viewModel,
                            points = points,
                            songLevels = songLevels
                        )
                        2 -> VenuesTab(
                            viewModel = viewModel,
                            points = points,
                            venueLevels = venueLevels
                        )
                        3 -> AchievementsTab(
                            unlockedAchievements = unlockedAchievements
                        )
                    }
                }
            }
        }
    }
}

// 1. Компонент Шапки
@Composable
fun HeaderSection(
    points: Double,
    pps: Double,
    clickPower: Double,
    totalClicks: Int,
    unlockedAchievementsCount: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .shadow(12.dp, RoundedCornerShape(24.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.08f),
                        Color.White.copy(alpha = 0.02f)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.15f),
                        Color.White.copy(alpha = 0.02f)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Текущие очки / Очки Рока
            Text(
                text = "ОЧКИ РОКА",
                fontSize = 11.sp,
                color = RockGrey,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                fontFamily = FontFamily.Monospace
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Крупный светящийся счетчик
            Text(
                text = points.toFormattedString(),
                fontSize = 32.sp,
                color = RockYellow,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Пассивный доход
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "ДОХОД", fontSize = 9.sp, color = RockGrey, fontWeight = FontWeight.Bold)
                    Text(
                        text = "+${pps.toFormattedString()}/с",
                        fontSize = 14.sp,
                        color = RockGreen,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Разделитель
                Box(modifier = Modifier.size(1.dp, 24.dp).background(RockBorder))

                // Сила клика
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "КЛИК", fontSize = 9.sp, color = RockGrey, fontWeight = FontWeight.Bold)
                    Text(
                        text = "+${clickPower.toFormattedString()}",
                        fontSize = 14.sp,
                        color = RockRed,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Разделитель
                Box(modifier = Modifier.size(1.dp, 24.dp).background(RockBorder))

                // Кликнули / Достижения
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "СТАТУС", fontSize = 9.sp, color = RockGrey, fontWeight = FontWeight.Bold)
                    Text(
                        text = "🏆 $unlockedAchievementsCount/${GameDefinitions.achievements.size}",
                        fontSize = 13.sp,
                        color = RockWhite,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

// 2. Компонент Зоны Кликера
@Composable
fun ClickZone(
    onTsoiClicked: (x: Float, y: Float) -> Unit,
    clickIndicators: List<ClickIndicator>,
    pps: Double
) {
    val haptic = LocalHapticFeedback.current
    var isClicked by remember { mutableStateOf(false) }

    // Анимация нажатия (сжатие картинки на долю секунды)
    val scaleFactor by animateFloatAsState(
        targetValue = if (isClicked) 0.90f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium),
        label = "click_scale"
    )

    // Анимация постоянного пульсирования в ритм музыки, темп зависит от PPS!
    val pulseDuration = when {
        pps <= 0 -> 2500
        pps < 10 -> 1800
        pps < 100 -> 1200
        pps < 1000 -> 800
        else -> 500
    }
    
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(pulseDuration, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ambient_pulse"
    )

    val finalScale = scaleFactor * pulseScale

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInputClickOnly { x, y ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onTsoiClicked(x, y)
                isClicked = true
            },
        contentAlignment = Alignment.Center
    ) {
        // Эффект свечения / Тень позади винила
        Box(
            modifier = Modifier
                .size(250.dp)
                .scale(finalScale)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            RockYellow.copy(alpha = 0.22f),
                            RockRed.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Конструкция виниловой пластинки (Круглый проигрыватель)
        Box(
            modifier = Modifier
                .size(220.dp)
                .scale(finalScale)
                .shadow(24.dp, CircleShape)
                .border(4.dp, RockYellow, CircleShape)
                .border(12.dp, Color.Black, CircleShape)
                .border(14.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                .clip(CircleShape)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            // Фоновое изображение Цоя (R.drawable.img_tsoi_hero)
            Image(
                painter = painterResource(id = R.drawable.img_tsoi_hero),
                contentDescription = "Виктор Цой",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            // Текстура дорожек винила поверх
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                    .border(25.dp, Color.Black.copy(alpha = 0.2f), CircleShape)
                    .border(45.dp, Color.White.copy(alpha = 0.05f), CircleShape)
                    .border(75.dp, Color.Black.copy(alpha = 0.3f), CircleShape)
            )
            
            // Центр пластинки (красный пятачок)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .border(2.dp, RockYellow, CircleShape)
                    .background(RockRed),
                contentAlignment = Alignment.Center
            ) {
                // Шпиндель
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(RockYellow, CircleShape)
                )
            }
        }

        // Рендерим всплывающие цифры (+X) поверх
        clickIndicators.forEach { indicator ->
            FloatingText(indicator = indicator)
        }
    }

    // Возвращаем в дефолтное состояние нажатия
    LaunchedEffect(isClicked) {
        if (isClicked) {
            delay(80)
            isClicked = false
        }
    }
}

// 3. Компоненты всплывающего текста
@Composable
fun FloatingText(indicator: ClickIndicator) {
    var animOffset by remember { mutableStateOf(0f) }
    var animAlpha by remember { mutableStateOf(1.0f) }

    LaunchedEffect(Unit) {
        animate(
            initialValue = 0f,
            targetValue = -130f,
            animationSpec = tween(durationMillis = 500, easing = LinearOutSlowInEasing)
        ) { value, _ ->
            animOffset = value
        }
    }
    
    LaunchedEffect(Unit) {
        animate(
            initialValue = 1.0f,
            targetValue = 0.0f,
            animationSpec = tween(durationMillis = 500, easing = FastOutLinearInEasing)
        ) { value, _ ->
            animAlpha = value
        }
    }

    Box(
        modifier = Modifier
            .offset(
                x = indicator.x.dp - 110.dp, // Центрируем примерно по ширине
                y = indicator.y.dp - 110.dp + animOffset.dp
            )
    ) {
        Text(
            text = indicator.text,
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            color = RockYellow,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.scale(1.2f),
            style = MaterialTheme.typography.bodyLarge.copy(
                shadow = androidx.compose.ui.graphics.Shadow(
                    color = Color.Black,
                    offset = androidx.compose.ui.geometry.Offset(2f, 4f),
                    blurRadius = 6f
                )
            ).copy(color = RockYellow.copy(alpha = animAlpha))
        )
    }
}

// Custom Modifier to handle only touch coordinate extraction for snappy clicks
fun Modifier.pointerInputClickOnly(onClick: (x: Float, y: Float) -> Unit): Modifier {
    return this.clickable(
        interactionSource = MutableInteractionSource(),
        indication = null
    ) {
        // Мы можем передать примерные координаты центра для простоты клика,
        // но добавим легкий разброс, чтобы цифры летели живописно!
        val randomX = (50..170).random().toFloat()
        val randomY = (50..170).random().toFloat()
        onClick(randomX, randomY)
    }
}

// Таб 0: Гитары и Люди
@Composable
fun GuitarsAndPeopleTab(
    viewModel: GameViewModel,
    points: Double,
    guitarLevel: Int,
    peopleLevels: List<Int>
) {
    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // РАЗДЕЛ: ГИТАРЫ
        item {
            SectionHeader(title = "ИНСТРУМЕНТ (УЛУЧШЕНИЕ КЛИКА)")
        }

        val nextGuitarLvl = guitarLevel + 1
        if (nextGuitarLvl < GameDefinitions.guitars.size) {
            val nextGuitar = GameDefinitions.guitars[nextGuitarLvl]
            val cost = viewModel.getGuitarCost(nextGuitarLvl)
            val isBuyable = points >= cost

            item {
                UpgradeItemRow(
                    title = nextGuitar.name,
                    subtitle = nextGuitar.description,
                    bonus = "+${nextGuitar.clickPower} к клику",
                    cost = cost,
                    isBuyable = isBuyable,
                    onBuyClick = { viewModel.buyGuitarUpgrade() },
                    iconRes = R.drawable.ic_launcher_foreground, // fallback/guitar icon decoration
                    isGuitar = true,
                    isHired = false
                )
            }
        } else {
            item {
                MaxUpgradeRow(title = "Ваша гитара: ${GameDefinitions.guitars.last().name}", desc = "Достигнут максимальный рок-уровень гитары!")
            }
        }

        // РАЗДЕЛ: ГРУППА & ОКРУЖЕНИЕ
        item {
            Spacer(modifier = Modifier.height(8.dp))
            SectionHeader(title = "МУЗЫКАНТЫ & ПОМОЩНИКИ (ПАССИВНЫЙ ДОХОД)")
        }

        items(GameDefinitions.people) { person ->
            val index = person.index
            val isHired = peopleLevels.getOrElse(index) { 0 } >= 1
            val cost = viewModel.getPersonCost(index)
            val isBuyable = points >= cost && !isHired

            UpgradeItemRow(
                title = person.name,
                subtitle = "${person.role} • ${person.description}",
                bonus = person.bonusDesc,
                cost = cost,
                isBuyable = isBuyable,
                onBuyClick = { viewModel.hirePerson(index) },
                iconRes = 0,
                isGuitar = false,
                isHired = isHired
            )
        }
    }
}

// Таб 1: Запись Песен
@Composable
fun SongsTab(
    viewModel: GameViewModel,
    points: Double,
    songLevels: List<Int>
) {
    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            SectionHeader(title = "ЗАПИСЬ ЛЕГЕНДАРНЫХ ПЕСЕН")
        }

        items(GameDefinitions.songs) { song ->
            val index = song.index
            val isRecorded = songLevels.getOrElse(index) { 0 } >= 1
            val cost = viewModel.getSongCost(index)
            val isBuyable = points >= cost && !isRecorded

            UpgradeItemRow(
                title = song.name,
                subtitle = song.description,
                bonus = "+${song.passiveIncome}/сек",
                cost = cost,
                isBuyable = isBuyable,
                onBuyClick = { viewModel.recordSong(index) },
                iconRes = 0,
                isGuitar = false,
                isHired = isRecorded,
                buttonText = "Записать"
            )
        }
    }
}

// Таб 2: Площадки
@Composable
fun VenuesTab(
    viewModel: GameViewModel,
    points: Double,
    venueLevels: List<Int>
) {
    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            SectionHeader(title = "ОРГАНИЗАЦИЯ КОНЦЕРТОВ")
        }

        items(GameDefinitions.venues) { venue ->
            val index = venue.index
            val count = venueLevels.getOrElse(index) { 0 }
            val cost = viewModel.getVenueCost(index)
            val isBuyable = points >= cost

            UpgradeItemRow(
                title = venue.name,
                subtitle = "${venue.description}\nКуплено: $count шт.",
                bonus = "+${venue.baseIncome}/сек каждая",
                cost = cost,
                isBuyable = isBuyable,
                onBuyClick = { viewModel.buyVenue(index) },
                iconRes = 0,
                isGuitar = false,
                isHired = false,
                buttonText = "Купить"
            )
        }
    }
}

// Таб 3: Достижения
@Composable
fun AchievementsTab(
    unlockedAchievements: Set<String>
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            SectionHeader(title = "ДОСТИЖЕНИЯ И ПРОГРЕСС")
        }

        items(GameDefinitions.achievements) { ach ->
            val isUnlocked = ach.id in unlockedAchievements
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = if (isUnlocked) RockYellow.copy(alpha = 0.5f) else RockBorder,
                        shape = RoundedCornerShape(8.dp)
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = if (isUnlocked) Color(0xFF24221A) else RockDarkGrey
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Иконка достижения (замочек или звездочка)
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                if (isUnlocked) RockYellow.copy(alpha = 0.2f) else RockBorder,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isUnlocked) Icons.Default.Star else Icons.Default.Lock,
                            contentDescription = if (isUnlocked) "Разблокировано" else "Заблокировано",
                            tint = if (isUnlocked) RockYellow else RockGrey,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = ach.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isUnlocked) RockYellow else RockGrey
                        )
                        Text(
                            text = ach.description,
                            fontSize = 11.sp,
                            color = if (isUnlocked) RockWhite else RockGrey,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }
    }
}

// Вспомогательные строки элементов списка
@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = RockRed,
        letterSpacing = 1.5.sp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 4.dp),
        fontFamily = FontFamily.Monospace
    )
}

@Composable
fun UpgradeItemRow(
    title: String,
    subtitle: String,
    bonus: String,
    cost: Double,
    isBuyable: Boolean,
    onBuyClick: () -> Unit,
    iconRes: Int = 0,
    isGuitar: Boolean = false,
    isHired: Boolean = false,
    buttonText: String = "Купить"
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.06f),
                        Color.White.copy(alpha = 0.01f)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.12f),
                        Color.White.copy(alpha = 0.02f)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Иконка/Символ слева
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                val iconVector = when {
                    isGuitar -> Icons.Default.Star
                    title.contains("Каспарян") || title.contains("Гурьянов") || title.contains("Тихомиров") -> Icons.Default.Person
                    buttonText == "Записать" -> Icons.Default.PlayArrow
                    else -> Icons.Default.Home
                }
                Icon(
                    imageVector = iconVector,
                    contentDescription = null,
                    tint = if (isHired) RockGreen else RockYellow,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Тексты описания посередине
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = RockWhite
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = RockGrey,
                    lineHeight = 14.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                // Отображение бонуса
                Text(
                    text = bonus,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = RockGreen
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Кнопка покупки справа
            if (isHired) {
                Button(
                    onClick = {},
                    enabled = false,
                    colors = ButtonDefaults.buttonColors(
                        disabledContainerColor = RockGreen.copy(alpha = 0.15f),
                        disabledContentColor = RockGreen
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Готово", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = onBuyClick,
                    enabled = isBuyable,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RockYellow,
                        contentColor = RockBlack,
                        disabledContainerColor = Color.White.copy(alpha = 0.05f),
                        disabledContentColor = RockGrey
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.widthIn(min = 95.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = buttonText.uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.SansSerif
                        )
                        Text(
                            text = cost.toFormattedCostString(),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MaxUpgradeRow(title: String, desc: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, RockYellow.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF21201D)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = RockYellow
            )
            Text(
                text = desc,
                fontSize = 11.sp,
                color = RockWhite
            )
        }
    }
}

// 4. Красивый диалог приветствия с оффлайн доходом
@Composable
fun OfflineEarningsDialog(
    earned: Double,
    seconds: Long,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF201F24),
                            Color(0xFF121215)
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .border(
                    width = 1.5.dp,
                    color = RockYellow,
                    shape = RoundedCornerShape(24.dp)
                ),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🎸 С ВОЗВРАЩЕНИЕМ! 🎸",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = RockYellow,
                    letterSpacing = 1.sp,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Пока вас не было в игре ${seconds.toFormattedDuration()}:",
                    fontSize = 12.sp,
                    color = RockWhite,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Записанные песни и собранные залы принесли вам:",
                    fontSize = 11.sp,
                    color = RockGrey,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Количество заработанных очков рока
                Text(
                    text = "+${earned.toFormattedString()} Очков Рока",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = RockGreen,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RockRed,
                        contentColor = RockWhite
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Забрать очки рока!",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            }
        }
    }
}

// Форматирование очков (вывод как 1.2M или 123,456)
fun Double.toFormattedString(): String {
    return when {
        this >= 1_000_000_000 -> String.format("%.2fB", this / 1_000_000_000.0)
        this >= 1_000_000 -> String.format("%.2fM", this / 1_000_000.0)
        this >= 10_000 -> String.format("%.1fk", this / 1_000.0)
        else -> this.roundToInt().toString()
    }
}

fun Double.toFormattedCostString(): String {
    return when {
        this >= 1_000_000_000 -> String.format("%.1fB", this / 1_000_000_000.0)
        this >= 1_000_000 -> String.format("%.1fM", this / 1_000_000.0)
        this >= 1_000 -> String.format("%.1fk", this / 1_000.0)
        else -> this.roundToInt().toString()
    }
}

fun Long.toFormattedDuration(): String {
    val mins = this / 60
    val hours = mins / 60
    val days = hours / 24

    return when {
        days > 0 -> "$days дн. ${hours % 24} ч."
        hours > 0 -> "$hours ч. ${mins % 60} мин."
        mins > 0 -> "$mins мин. ${this % 60} сек."
        else -> "$this сек."
    }
}
