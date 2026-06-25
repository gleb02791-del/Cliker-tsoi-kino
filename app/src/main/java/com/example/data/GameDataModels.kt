package com.example.data

// Гитара (улучшение клика)
data class GuitarLevel(
    val level: Int,
    val name: String,
    val description: String,
    val clickPower: Int,
    val cost: Double
)

// Площадки (пассивный доход)
data class Venue(
    val index: Int,
    val id: String,
    val name: String,
    val description: String,
    val baseCost: Double,
    val baseIncome: Double // в секунду
) {
    fun getCost(count: Int): Double {
        // Стоимость растет экспоненциально
        return baseCost * Math.pow(1.15, count.toDouble())
    }
}

// Песни (пассивный доход)
data class SongRecord(
    val index: Int,
    val id: String,
    val name: String,
    val description: String,
    val cost: Double,
    val passiveIncome: Double
)

// Персонажи (участники и окружение)
data class BandMember(
    val index: Int,
    val id: String,
    val name: String,
    val role: String,
    val description: String,
    val cost: Double,
    val bonusDesc: String
)

// Достижения
data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val checkCondition: (points: Double, totalClicks: Int, guitarLvl: Int, venues: List<Int>, songs: List<Int>, people: List<Int>) -> Boolean
)

object GameDefinitions {
    val guitars = listOf(
        GuitarLevel(0, "Акустика «Ленинград»", "Обычная советская акустическая гитара с легким дребезжанием.", 1, 0.0),
        GuitarLevel(1, "12-струнная гитара", "Звучит плотнее и звонче, как на квартирниках.", 4, 150.0),
        GuitarLevel(2, "Электрогитара «Урал»", "Легендарный советский инструмент с железным характером.", 15, 1200.0),
        GuitarLevel(3, "Белый Kramer Цоя", "Стильный импортный инструмент для ярких соло.", 70, 8000.0),
        GuitarLevel(4, "Fender Stratocaster", "Мечта любого советского рокера, чистейший звук.", 350, 45000.0),
        GuitarLevel(5, "Золотой Gibson Les Paul", "Абсолютный рок-трофей для истинного лидера Кино.", 2000, 250000.0)
    )

    val venues = listOf(
        Venue(0, "kamchatka", "Кочегарка «Камчатка»", "Угольная котельная в Ленинграде, где Цой работал кочегаром. Место силы.", 60.0, 0.5),
        Venue(1, "rock_club", "Ленинградский Рок-Клуб", "Культовая площадка на Рубинштейна, 13. Первый шаг к успеху.", 450.0, 3.0),
        Venue(2, "olimpiyskiy", "СК «Олимпийский»", "Полный стадион ревущих фанатов в Москве. Огромный размах!", 3500.0, 20.0),
        Venue(3, "luzhniki", "Стадион «Лужники»", "Легендарная площадка последнего грандиозного концерта с зажжением чаши.", 30000.0, 150.0)
    )

    val songs = listOf(
        SongRecord(0, "zvezda", "«Звезда по имени Солнце»", "Песня о вечной борьбе и небесном светиле.", 250.0, 2.5),
        SongRecord(1, "pack", "«Пачка сигарет»", "И если есть в кармане пачка сигарет, значит всё не так уж плохо...", 1800.0, 15.0),
        SongRecord(2, "blood", "«Группа крови»", "Настоящий гимн поколения. Пожелай мне удачи в бою!", 12000.0, 100.0),
        SongRecord(3, "peremen", "«Хочу перемен!»", "Энергетический взрыв, требующий изменений в сердцах.", 85000.0, 600.0),
        SongRecord(4, "kukushka", "«Кукушка»", "Глубокая философская притча о жизни, судьбе и времени.", 600000.0, 4000.0)
    )

    val people = listOf(
        BandMember(
            index = 0,
            id = "kasparyan",
            name = "Юрий Каспарян",
            role = "Соло-гитара",
            description = "Главный гитарист группы Кино, создатель незабываемых риффов.",
            cost = 800.0,
            bonusDesc = "+25% к силе клика"
        ),
        BandMember(
            index = 1,
            id = "guryanov",
            name = "Георгий Гурьянов",
            role = "Барабаны / «Густав»",
            description = "Стильный драм-машина и художник, задающий безупречный ритм.",
            cost = 4000.0,
            bonusDesc = "+10 к клику и автоклик раз в 2 сек."
        ),
        BandMember(
            index = 2,
            id = "tikhomirov",
            name = "Игорь Тихомиров",
            role = "Бас-гитара",
            description = "Профессиональный басист, добавивший плотности звучанию.",
            cost = 20000.0,
            bonusDesc = "Экономия 12% на покупку гитар и площадок"
        ),
        BandMember(
            index = 3,
            id = "marianna",
            name = "Марьяна Цой",
            role = "Продюсер / Жена",
            description = "Администратор, гример и преданный двигатель группы вперед.",
            cost = 150000.0,
            bonusDesc = "+30% ко всему пассивному доходу"
        ),
        BandMember(
            index = 4,
            id = "joanna",
            name = "Джоанна Стингрей",
            role = "Промоутер из США",
            description = "Американская подруга, вывезшая записи Кино на Запад.",
            cost = 900000.0,
            bonusDesc = "+50% пассивного дохода от записанных песен"
        )
    )

    val achievements = listOf(
        Achievement("first_chord", "Первый аккорд", "Сделайте свой самый первый клик по Цою.") { _, clicks, _, _, _, _ ->
            clicks >= 1
        },
        Achievement("aluminum_cucumbers", "Алюминиевые огурцы", "Посадите 100 алюминиевых огурцов на брезентовом поле (100 кликов).") { _, clicks, _, _, _, _ ->
            clicks >= 100
        },
        Achievement("guitar_hero", "Гитарный виртуоз", "Приобретите Fender Stratocaster или выше.") { _, _, guitarLvl, _, _, _ ->
            guitarLvl >= 4
        },
        Achievement("kamchatka_work", "Жизнь в котельной", "Купите хотя бы одну котельную «Камчатка».") { _, _, _, venues, _, _ ->
            venues.getOrElse(0) { 0 } >= 1
        },
        Achievement("full_band", "Группа в сборе!", "Примите в группу Каспаряна, Гурьянова и Тихомирова.") { _, _, _, _, _, people ->
            people.getOrElse(0) { 0 } >= 1 && people.getOrElse(1) { 0 } >= 1 && people.getOrElse(2) { 0 } >= 1
        },
        Achievement("legend_album", "Студийная легенда", "Запишите все 5 легендарных песен Кино.") { _, _, _, _, songs, _ ->
            songs.all { it >= 1 }
        },
        Achievement("rock_emperor", "Легенда рок-н-ролла", "Накопите суммарно 1,000,000 очков рока.") { _, _, _, _, _, _ ->
            // Специфический чек на points или totalPointsEarned, проверим по обоим
            false // Мы проверим это вручную в ViewModel на основе накопленного
        }
    )
}
