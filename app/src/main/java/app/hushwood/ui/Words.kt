package app.hushwood.ui

import app.hushwood.domain.Beast
import app.hushwood.domain.Breath
import app.hushwood.domain.Daypart
import app.hushwood.domain.Gift
import app.hushwood.domain.Kind
import app.hushwood.domain.Offering
import app.hushwood.domain.Place
import app.hushwood.domain.Season
import app.hushwood.domain.Spot
import app.hushwood.domain.Trust

/** A line in both languages. The forest's words live here, the buttons' in strings.xml. */
class W(val en: String, val ru: String) {
    fun of(ru: Boolean) = if (ru) this.ru else en
}

/** Everything the forest says, in English and Russian. Russian lines avoid gendered past forms. */
object Words {
    fun name(b: Beast): W = when (b) {
        Beast.TIT -> W("Great tit", "Синица")
        Beast.ROBIN -> W("Robin", "Зарянка")
        Beast.WREN -> W("Wren", "Крапивник")
        Beast.SQUIRREL -> W("Squirrel", "Белка")
        Beast.HEDGEHOG -> W("Hedgehog", "Ёж")
        Beast.HARE -> W("Hare", "Заяц")
        Beast.OWL -> W("Eagle owl", "Филин")
        Beast.FOX -> W("Fox", "Лиса")
        Beast.DEER -> W("Roe deer", "Косуля")
        Beast.BEAR -> W("Brown bear", "Бурый медведь")
        Beast.FIREFLIES -> W("Fireflies", "Светлячки")
        Beast.WHITE_DEER -> W("White deer", "Белый олень")
        Beast.DUCK -> W("Mallard", "Кряква")
        Beast.FROG -> W("Frog", "Лягушка")
        Beast.DRAGONFLY -> W("Dragonflies", "Стрекозы")
        Beast.HERON -> W("Grey heron", "Серая цапля")
        Beast.BEAVER -> W("Beaver", "Бобр")
        Beast.SWAN -> W("Whooper swan", "Лебедь")
        Beast.OTTER -> W("Otter", "Выдра")
        Beast.CRANE -> W("Common crane", "Серый журавль")
        Beast.MOOSE -> W("Moose", "Лось")
        Beast.KINGFISHER -> W("Kingfisher", "Зимородок")
        Beast.CRANES -> W("A skein of cranes", "Журавлиный клин")
        Beast.CHOUGH -> W("Alpine chough", "Альпийская галка")
        Beast.PIKA -> W("Pika", "Пищуха")
        Beast.BUTTERFLY -> W("Apollo butterflies", "Аполлоны")
        Beast.MARMOT -> W("Marmot", "Сурок")
        Beast.EAGLE -> W("Golden eagle", "Беркут")
        Beast.IBEX -> W("Ibex", "Козерог")
        Beast.LYNX -> W("Lynx", "Рысь")
        Beast.SNOW_LEOPARD -> W("Snow leopard", "Снежный барс")
        Beast.CRAB -> W("Shore crab", "Краб")
        Beast.SANDPIPER -> W("Sandpiper", "Песочник")
        Beast.GULL -> W("Herring gull", "Серебристая чайка")
        Beast.PUFFIN -> W("Puffin", "Тупик")
        Beast.SEAL -> W("Grey seal", "Тюлень")
        Beast.SPARKLE -> W("Glowing sea", "Свечение моря")
        Beast.WHALE -> W("Whale", "Кит")
    }

    fun lore(b: Beast): W = when (b) {
        Beast.TIT -> W("The first to come and the last to leave. It doesn't mind you at all.", "Приходит первой и уходит последней. Тебя она совсем не боится.")
        Beast.ROBIN -> W("Sings at the edges of the day, and watches what your hands do.", "Поёт на краях дня и следит, что делают твои руки.")
        Beast.WREN -> W("Tiny, loud, tail up. Comes to those who come back every morning.", "Крошечный, громкий, хвост торчком. Приходит к тем, кто возвращается каждое утро.")
        Beast.SQUIRREL -> W("Busy, but it will stop for someone who isn't.", "Вечно занята, но остановится рядом с тем, кто никуда не спешит.")
        Beast.HEDGEHOG -> W("Snuffles about at dusk. Likes an evening with a notebook.", "Сопит в сумерках. Любит вечер с блокнотом.")
        Beast.HARE -> W("All ears. One movement and it's gone; stillness, and it sits a long time.", "Весь в ушах. Одно движение — и нет его; замри — и он долго сидит рядом.")
        Beast.OWL -> W("Only at night. It has been watching you longer than you think.", "Только ночью. Он смотрит на тебя дольше, чем кажется.")
        Beast.FOX -> W("Comes at odd hours, like you. Curious, never in a hurry.", "Приходит в разное время, как и ты. Любопытная и никуда не спешит.")
        Beast.DEER -> W("Steps out of the mist at dawn and dusk, when the clearing is quiet enough.", "Выходит из тумана на рассвете и в сумерках, когда на поляне достаточно тихо.")
        Beast.BEAR -> W("Rarely seen, long remembered. Patient with the patient.", "Его редко видят и долго помнят. Терпелив с терпеливыми.")
        Beast.FIREFLIES -> W("Summer nights. They gather where someone sits still.", "Летние ночи. Собираются там, где кто-то сидит тихо.")
        Beast.WHITE_DEER -> W("They say it comes at dawn to someone who has sat for half an hour without moving.", "Говорят, он выходит на рассвете к тому, кто полчаса просидел не шелохнувшись.")
        Beast.DUCK -> W("Drifts by, dabbles, drifts back. Water is never in a hurry.", "Проплывёт, нырнёт, проплывёт обратно. Вода никогда не спешит.")
        Beast.FROG -> W("A small round stillness on the bank. It can wait longer than you.", "Маленькая круглая неподвижность на берегу. Ждать она умеет дольше тебя.")
        Beast.DRAGONFLY -> W("Summer noon over the water, blue and quick.", "Летний полдень над водой, синие и быстрые.")
        Beast.HERON -> W("Stands on one leg in the shallows for as long as it takes. Your teacher.", "Стоит на мелководье на одной ноге столько, сколько нужно. Твой учитель.")
        Beast.BEAVER -> W("Works at dusk. If you're quiet, it will work right next to you.", "Работает в сумерках. Если тихо, будет работать прямо рядом.")
        Beast.SWAN -> W("Glides out from the reeds when the water is smooth.", "Выплывает из камышей, когда вода гладкая.")
        Beast.OTTER -> W("Plays more than it hunts. Shy of people, not of quiet ones.", "Больше играет, чем охотится. Боится людей, но не тихих.")
        Beast.CRANE -> W("Dances in spring. Comes close to those who move slowly.", "Танцует весной. Подходит к тем, кто двигается медленно.")
        Beast.MOOSE -> W("Walks into the lake at dawn as if it owned it. It does.", "Входит в озеро на рассвете, будто оно его. Так и есть.")
        Beast.KINGFISHER -> W("A blue spark by the water, for someone who comes to breathe here seven days of ten.", "Синяя искра у воды — для того, кто приходит сюда дышать семь дней из десяти.")
        Beast.CRANES -> W("Pass over around the equinoxes, calling.", "Пролетают над озером в дни равноденствия и кричат.")
        Beast.CHOUGH -> W("Black, yellow-billed, bold. Will share your lunch.", "Чёрная, с жёлтым клювом, нахальная. Разделит с тобой обед.")
        Beast.PIKA -> W("Gathers flowers for the winter and squeaks at the wind.", "Собирает цветы на зиму и пищит на ветер.")
        Beast.BUTTERFLY -> W("White wings with red eyes, only on summer days high up.", "Белые крылья с красными глазками, только летом и только высоко.")
        Beast.MARMOT -> W("Stands up and looks around. When it whistles, everything listens.", "Встаёт столбиком и оглядывается. Когда свистит, слушают все.")
        Beast.EAGLE -> W("Circles above for a long time before it lets itself be seen.", "Долго кружит в вышине, прежде чем позволит себя увидеть.")
        Beast.IBEX -> W("On ledges no one else would stand on, perfectly calm.", "На уступах, где никто больше не устоит, и совершенно спокоен.")
        Beast.LYNX -> W("You will see it only after it has seen you for a long while.", "Ты увидишь её, только когда она давно на тебя смотрит.")
        Beast.SNOW_LEOPARD -> W("A ghost of the winter mountains. Comes to the very still at dusk.", "Призрак зимних гор. Приходит в сумерках к очень тихим.")
        Beast.CRAB -> W("Sideways, sideways, gone. Back when you are still.", "Бочком, бочком — и нет. Вернётся, когда замрёшь.")
        Beast.SANDPIPER -> W("Runs after each wave and away from the next.", "Бежит за каждой волной и от следующей.")
        Beast.GULL -> W("Loud, sure of itself, and fond of fires.", "Громкая, уверенная и любит костры.")
        Beast.PUFFIN -> W("Sits on the cliff in spring with its striped beak, like a small clown.", "Весной сидит на утёсе с полосатым клювом, как маленький клоун.")
        Beast.SEAL -> W("Hauls out on the rocks and looks at you with a dog's eyes.", "Выбирается на камни и смотрит на тебя собачьими глазами.")
        Beast.SPARKLE -> W("Late summer nights: the waves break in blue light.", "Поздним летом ночью волны разбиваются голубым светом.")
        Beast.WHALE -> W("A spout far out, for someone who has looked at the sea for twenty minutes.", "Фонтан далеко в море — для того, кто двадцать минут смотрит на воду.")
    }

    fun place(p: Place): W = when (p) {
        Place.FOREST -> W("Forest clearing", "Лесная опушка")
        Place.LAKE -> W("The lake", "Озеро")
        Place.MOUNTAINS -> W("Mountain meadow", "Горный луг")
        Place.SEA -> W("The shore", "Морской берег")
    }

    fun placeLine(p: Place): W = when (p) {
        Place.FOREST -> W("Spruces, birches, a stump to sit on.", "Ели, берёзы и пень, чтобы сесть.")
        Place.LAKE -> W("A wooden pier, reeds, still water.", "Деревянные мостки, камыши, тихая вода.")
        Place.MOUNTAINS -> W("Snow on the peaks, flowers at your feet.", "Снег на вершинах, цветы под ногами.")
        Place.SEA -> W("Dunes, a cliff, the slow breathing of the waves.", "Дюны, утёс и медленное дыхание волн.")
    }

    fun spot(p: Place, s: Spot): W = when (p) {
        Place.FOREST -> when (s) {
            Spot.STUMP -> W("on the stump", "на пне")
            Spot.STREAM -> W("by the stream", "у ручья")
            Spot.STONE -> W("on the stone", "на камне")
            Spot.FIRE -> W("by the fire", "у костра")
            Spot.PATH -> W("on the path", "на тропе")
        }
        Place.LAKE -> when (s) {
            Spot.STUMP -> W("on the log", "на бревне")
            Spot.STREAM -> W("on the pier", "на мостках")
            Spot.STONE -> W("on the boulder", "на валуне")
            Spot.FIRE -> W("by the fire", "у костра")
            Spot.PATH -> W("along the shore", "вдоль берега")
        }
        Place.MOUNTAINS -> when (s) {
            Spot.STUMP -> W("on the flat rock", "на плоском камне")
            Spot.STREAM -> W("by the brook", "у ручья")
            Spot.STONE -> W("on the boulder", "на валуне")
            Spot.FIRE -> W("by the fire", "у костра")
            Spot.PATH -> W("on the trail", "на тропе")
        }
        Place.SEA -> when (s) {
            Spot.STUMP -> W("on the driftwood", "на коряге")
            Spot.STREAM -> W("by the tide pools", "у луж прилива")
            Spot.STONE -> W("on the cliff", "на утёсе")
            Spot.FIRE -> W("by the fire", "у костра")
            Spot.PATH -> W("through the dunes", "через дюны")
        }
    }

    fun kind(k: Kind): W = when (k) {
        Kind.SIT -> W("Sitting", "Сидя")
        Kind.BREATH -> W("Breathing", "Дыхание")
        Kind.MOVE -> W("Movement", "Движение")
        Kind.WRITE -> W("Writing", "Письмо")
        Kind.WALK -> W("Walking", "Прогулка")
        Kind.OTHER -> W("Other", "Другое")
    }

    /** The practices offered on the first day, each with its kind. */
    val starters = listOf(
        W("Meditation", "Медитация") to Kind.SIT,
        W("Breathing", "Дыхание") to Kind.BREATH,
        W("Yoga", "Йога") to Kind.MOVE,
        W("Journal", "Дневник") to Kind.WRITE,
        W("Mindful walk", "Прогулка") to Kind.WALK,
        W("Prayer", "Молитва") to Kind.SIT,
        W("Stretching", "Растяжка") to Kind.MOVE,
        W("Gratitude", "Благодарность") to Kind.WRITE,
    )

    fun part(p: Daypart): W = when (p) {
        Daypart.DAWN -> W("dawn", "рассвет")
        Daypart.DAY -> W("day", "день")
        Daypart.DUSK -> W("dusk", "сумерки")
        Daypart.NIGHT -> W("night", "ночь")
    }

    fun season(s: Season): W = when (s) {
        Season.SPRING -> W("spring", "весна")
        Season.SUMMER -> W("summer", "лето")
        Season.AUTUMN -> W("autumn", "осень")
        Season.WINTER -> W("winter", "зима")
    }

    fun offering(o: Offering): W = when (o) {
        Offering.SEEDS -> W("Seeds", "Семечки")
        Offering.BERRIES -> W("Berries", "Ягоды")
        Offering.NUTS -> W("Nuts", "Орехи")
        Offering.APPLE -> W("An apple", "Яблоко")
        Offering.HONEY -> W("Honey", "Мёд")
    }

    fun gift(g: Gift): W = when (g) {
        Gift.FEATHER -> W("a feather", "перо")
        Gift.ACORN -> W("an acorn", "жёлудь")
        Gift.PEBBLE -> W("a smooth pebble", "гладкий камешек")
        Gift.PINECONE -> W("a pinecone", "шишку")
        Gift.FERN -> W("a fern leaf", "лист папоротника")
        Gift.ROWAN -> W("a rowan sprig", "веточку рябины")
        Gift.SHELL -> W("a shell", "ракушку")
        Gift.MOSS -> W("a tuft of moss", "кусочек мха")
    }

    fun breath(b: Breath): W = when (b) {
        Breath.CALM -> W("Calm · 4–6", "Спокойное · 4–6")
        Breath.EVEN -> W("Even · 5–5", "Ровное · 5–5")
        Breath.BOX -> W("Box · 4–4–4–4", "Квадрат · 4–4–4–4")
        Breath.RELAX -> W("Sleep · 4–7–8", "Ко сну · 4–7–8")
    }

    fun phase(p: Breath.Phase): W = when (p) {
        Breath.Phase.IN -> W("breathe in", "вдох")
        Breath.Phase.HOLD -> W("hold", "задержка")
        Breath.Phase.OUT -> W("breathe out", "выдох")
        Breath.Phase.REST -> W("rest", "пауза")
    }

    /** The guardian: what quality it stands for, and why it chose you. */
    fun quality(b: Beast): W = when (b) {
        Beast.WREN -> W("Constancy", "Постоянство")
        Beast.OWL -> W("Depth", "Глубина")
        Beast.HERON -> W("Patience", "Терпение")
        Beast.CRANE -> W("Balance", "Равновесие")
        Beast.BEAR -> W("The strength to return", "Сила возвращения")
        Beast.FOX -> W("Flexibility", "Гибкость")
        Beast.HEDGEHOG -> W("Gentleness", "Бережность")
        Beast.DEER -> W("Sensitivity", "Чуткость")
        else -> W("", "")
    }

    fun why(b: Beast): W = when (b) {
        Beast.WREN -> W("You come early and you come often. A little every morning: that's the wren's way.", "Ты приходишь рано и часто. Понемногу каждое утро — это путь крапивника.")
        Beast.OWL -> W("You sit long and late, when everyone else is asleep.", "Ты сидишь долго и поздно, когда все уже спят.")
        Beast.HERON -> W("You breathe by the water and stay perfectly still.", "Ты дышишь у воды и умеешь замереть.")
        Beast.CRANE -> W("Your practice moves: stretching, walking, standing on one leg.", "Твоя практика в движении: растяжка, прогулки, стойка на одной ноге.")
        Beast.BEAR -> W("You don't come every day, but when you do, you stay a long time.", "Ты приходишь не каждый день, но если пришёл — то надолго.")
        Beast.FOX -> W("You come at any hour the day allows. That's not chaos, that's the fox's way.", "Ты приходишь тогда, когда позволяет день. Это не хаос, это лисий путь.")
        Beast.HEDGEHOG -> W("Your evenings: a quiet fire, a notebook, a line about the day.", "Твои вечера — тихий костёр, блокнот, строчка о прожитом дне.")
        Beast.DEER -> W("You practise in many ways, sometimes several times a day.", "Ты практикуешь по-разному, иногда по нескольку раз в день.")
        else -> W("", "")
    }

    fun hint(b: Beast): W = when (b) {
        Beast.WREN -> W("Try three minutes right after waking.", "Попробуй три минуты сразу после пробуждения.")
        Beast.OWL -> W("Try sitting in the dark, without the screen.", "Попробуй посидеть в темноте, без экрана.")
        Beast.HERON -> W("Try making the out-breath twice as long.", "Попробуй сделать выдох вдвое длиннее вдоха.")
        Beast.CRANE -> W("Try standing on one leg for a minute, eyes soft.", "Попробуй минуту постоять на одной ноге, мягко глядя вперёд.")
        Beast.BEAR -> W("Try a short sit on a day you think you have no time.", "Попробуй короткую сессию в день, когда кажется, что времени нет.")
        Beast.FOX -> W("Try sitting wherever you are, just for two minutes.", "Попробуй посидеть прямо там, где ты сейчас, всего две минуты.")
        Beast.HEDGEHOG -> W("Try writing one line about what you felt today.", "Попробуй записать одну строчку о том, что ты сегодня чувствуешь.")
        Beast.DEER -> W("Try one breath of attention between two things.", "Попробуй один осознанный вдох между двумя делами.")
        else -> W("", "")
    }

    fun trust(t: Trust): W = when (t) {
        Trust.WATCHING -> W("watches you from the bushes", "смотрит на тебя из кустов")
        Trust.EDGE -> W("waits at the edge", "ждёт на краю")
        Trust.NEAR -> W("comes halfway to you", "подходит на полпути")
        Trust.BESIDE -> W("sleeps beside you", "спит рядом с тобой")
    }

    /** What the place says on the main screen, picked by the hour. */
    val idle = listOf(
        W("Sit quietly, and the forest will come to you.", "Сиди тихо — и лес выйдет к тебе."),
        W("Nobody here is in a hurry.", "Здесь никто никуда не спешит."),
        W("The grass rustles. Someone is waiting to see if you'll stay.", "Шуршит трава. Кто-то ждёт, останешься ли ты."),
        W("Stillness is an invitation.", "Тишина — это приглашение."),
        W("You can come back here at any time.", "Сюда можно вернуться в любой момент."),
        W("The wind has settled. It's quiet enough.", "Ветер улёгся. Тихо как раз настолько, насколько нужно."),
    )

    /** What the clearing says while you sit. */
    fun calm(quiet: Float, moving: Boolean): W = when {
        moving -> W("They run back into the trees. They'll be back.", "Убежали в лес. Они вернутся.")
        quiet < 0.5f -> W("Settle in. Let the phone lie still.", "Устройся поудобнее. Пусть телефон лежит тихо.")
        quiet < 3f -> W("It's getting quiet.", "Становится тихо.")
        quiet < 10f -> W("Something rustles in the grass.", "Что-то шуршит в траве.")
        quiet < 20f -> W("The clearing has forgotten you're here.", "Поляна забыла, что ты здесь.")
        else -> W("Deep quiet. Anything can come out now.", "Глубокая тишина. Теперь может выйти кто угодно.")
    }
}
