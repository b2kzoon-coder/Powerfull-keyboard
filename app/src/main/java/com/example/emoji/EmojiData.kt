package com.example.emoji

/**
 * Complete offline catalog of Unicode emojis categorized according to Unicode standards
 * and Ridmik Classic keyboard layout. Includes keywords for search and skin tone indicators.
 */
object EmojiData {

  private val smileys: List<EmojiItem> by lazy(LazyThreadSafetyMode.PUBLICATION) {
    listOf(
    EmojiItem("😀", "grinning face", EmojiCategory.SMILEYS, listOf("smile", "happy", "grin")),
    EmojiItem("😃", "grinning face with big eyes", EmojiCategory.SMILEYS, listOf("smile", "happy", "joy")),
    EmojiItem("😄", "grinning face with smiling eyes", EmojiCategory.SMILEYS, listOf("smile", "happy", "laugh")),
    EmojiItem("😁", "beaming face with smiling eyes", EmojiCategory.SMILEYS, listOf("grin", "smile", "teeth")),
    EmojiItem("😆", "grinning squinting face", EmojiCategory.SMILEYS, listOf("laugh", "haha", "squint")),
    EmojiItem("😅", "grinning face with sweat", EmojiCategory.SMILEYS, listOf("sweat", "nervous", "relief")),
    EmojiItem("🤣", "rolling on the floor laughing", EmojiCategory.SMILEYS, listOf("rofl", "lol", "laugh")),
    EmojiItem("😂", "face with tears of joy", EmojiCategory.SMILEYS, listOf("cry", "tears", "laugh", "happy")),
    EmojiItem("🙂", "slightly smiling face", EmojiCategory.SMILEYS, listOf("smile", "ok", "content")),
    EmojiItem("🙃", "upside-down face", EmojiCategory.SMILEYS, listOf("sarcasm", "irony", "silly")),
    EmojiItem("😉", "winking face", EmojiCategory.SMILEYS, listOf("wink", "flirt", "joke")),
    EmojiItem("😊", "smiling face with smiling eyes", EmojiCategory.SMILEYS, listOf("blush", "smile", "warm")),
    EmojiItem("😇", "smiling face with halo", EmojiCategory.SMILEYS, listOf("angel", "innocent", "halo")),
    EmojiItem("🥰", "smiling face with hearts", EmojiCategory.SMILEYS, listOf("love", "adore", "crush", "hearts")),
    EmojiItem("😍", "smiling face with heart-eyes", EmojiCategory.SMILEYS, listOf("love", "heart", "eyes", "crush")),
    EmojiItem("🤩", "star-struck", EmojiCategory.SMILEYS, listOf("star", "eyes", "wow", "excited")),
    EmojiItem("😘", "face blowing a kiss", EmojiCategory.SMILEYS, listOf("kiss", "love", "heart")),
    EmojiItem("😗", "kissing face", EmojiCategory.SMILEYS, listOf("kiss", "whistle")),
    EmojiItem("😚", "kissing face with closed eyes", EmojiCategory.SMILEYS, listOf("kiss", "blush")),
    EmojiItem("😙", "kissing face with smiling eyes", EmojiCategory.SMILEYS, listOf("kiss", "smile")),
    EmojiItem("😋", "face savoring food", EmojiCategory.SMILEYS, listOf("yum", "delicious", "tongue")),
    EmojiItem("😛", "face with tongue", EmojiCategory.SMILEYS, listOf("tongue", "silly", "playful")),
    EmojiItem("😜", "winking face with tongue", EmojiCategory.SMILEYS, listOf("wink", "tongue", "crazy")),
    EmojiItem("🤪", "zany face", EmojiCategory.SMILEYS, listOf("crazy", "wild", "goofy")),
    EmojiItem("😝", "squinting face with tongue", EmojiCategory.SMILEYS, listOf("tongue", "playful", "haha")),
    EmojiItem("🤑", "money-mouth face", EmojiCategory.SMILEYS, listOf("money", "dollar", "rich")),
    EmojiItem("🤗", "smiling face with open hands", EmojiCategory.SMILEYS, listOf("hug", "warm", "open")),
    EmojiItem("🤭", "face with hand over mouth", EmojiCategory.SMILEYS, listOf("oops", "giggle", "shy")),
    EmojiItem("🤫", "shushing face", EmojiCategory.SMILEYS, listOf("quiet", "shh", "secret")),
    EmojiItem("🤔", "thinking face", EmojiCategory.SMILEYS, listOf("think", "wonder", "hmm")),
    EmojiItem("🤐", "zipper-mouth face", EmojiCategory.SMILEYS, listOf("zip", "silent", "secret")),
    EmojiItem("🤨", "face with raised eyebrow", EmojiCategory.SMILEYS, listOf("skeptical", "suspicious", "doubt")),
    EmojiItem("😐", "neutral face", EmojiCategory.SMILEYS, listOf("meh", "neutral", "blank")),
    EmojiItem("😑", "expressionless face", EmojiCategory.SMILEYS, listOf("expressionless", "straight", "unimpressed")),
    EmojiItem("😶", "face without mouth", EmojiCategory.SMILEYS, listOf("mute", "speechless", "quiet")),
    EmojiItem("😏", "smirking face", EmojiCategory.SMILEYS, listOf("smirk", "flirt", "sly")),
    EmojiItem("😒", "unamused face", EmojiCategory.SMILEYS, listOf("unamused", "bored", "annoyed")),
    EmojiItem("🙄", "face with rolling eyes", EmojiCategory.SMILEYS, listOf("eyeroll", "whatever", "annoyed")),
    EmojiItem("😬", "grimacing face", EmojiCategory.SMILEYS, listOf("grimace", "awkward", "nervous")),
    EmojiItem("🤥", "lying face", EmojiCategory.SMILEYS, listOf("lie", "pinocchio", "fake")),
    EmojiItem("😌", "relieved face", EmojiCategory.SMILEYS, listOf("relief", "peace", "calm")),
    EmojiItem("😔", "pensive face", EmojiCategory.SMILEYS, listOf("sad", "pensive", "down")),
    EmojiItem("😪", "sleepy face", EmojiCategory.SMILEYS, listOf("sleepy", "tired", "rest")),
    EmojiItem("🤤", "drooling face", EmojiCategory.SMILEYS, listOf("drool", "hungry", "delicious")),
    EmojiItem("😴", "sleeping face", EmojiCategory.SMILEYS, listOf("sleep", "zzz", "tired")),
    EmojiItem("😷", "face with medical mask", EmojiCategory.SMILEYS, listOf("mask", "sick", "virus")),
    EmojiItem("🤒", "face with thermometer", EmojiCategory.SMILEYS, listOf("sick", "fever", "ill")),
    EmojiItem("🤕", "face with head-bandage", EmojiCategory.SMILEYS, listOf("hurt", "injury", "pain")),
    EmojiItem("🤢", "nauseated face", EmojiCategory.SMILEYS, listOf("sick", "gross", "nausea")),
    EmojiItem("🤮", "face vomiting", EmojiCategory.SMILEYS, listOf("vomit", "barf", "disgust")),
    EmojiItem("🤧", "sneezing face", EmojiCategory.SMILEYS, listOf("sneeze", "cold", "tissue")),
    EmojiItem("🥵", "hot face", EmojiCategory.SMILEYS, listOf("hot", "heat", "sweat")),
    EmojiItem("🥶", "cold face", EmojiCategory.SMILEYS, listOf("cold", "freezing", "ice")),
    EmojiItem("🥴", "woozy face", EmojiCategory.SMILEYS, listOf("drunk", "woozy", "dizzy")),
    EmojiItem("😵", "knocked-out face", EmojiCategory.SMILEYS, listOf("dizzy", "dead", "shock")),
    EmojiItem("🤯", "exploding head", EmojiCategory.SMILEYS, listOf("mindblown", "boom", "shock")),
    EmojiItem("🤠", "cowboy hat face", EmojiCategory.SMILEYS, listOf("cowboy", "hat", "country")),
    EmojiItem("🥳", "partying face", EmojiCategory.SMILEYS, listOf("party", "celebration", "birthday")),
    EmojiItem("😎", "smiling face with sunglasses", EmojiCategory.SMILEYS, listOf("cool", "sunglasses", "chill")),
    EmojiItem("🤓", "nerd face", EmojiCategory.SMILEYS, listOf("nerd", "geek", "glasses")),
    EmojiItem("🧐", "face with monocle", EmojiCategory.SMILEYS, listOf("monocle", "curious", "inspect")),
    EmojiItem("😕", "confused face", EmojiCategory.SMILEYS, listOf("confused", "puzzled", "unsure")),
    EmojiItem("😟", "worried face", EmojiCategory.SMILEYS, listOf("worried", "nervous", "anxious")),
    EmojiItem("🙁", "slightly frowning face", EmojiCategory.SMILEYS, listOf("frown", "sad", "unhappy")),
    EmojiItem("☹️", "frowning face", EmojiCategory.SMILEYS, listOf("sad", "frown", "upset")),
    EmojiItem("😮", "face with open mouth", EmojiCategory.SMILEYS, listOf("wow", "surprised", "open")),
    EmojiItem("😯", "hushed face", EmojiCategory.SMILEYS, listOf("hushed", "quiet", "stunned")),
    EmojiItem("😲", "astonished face", EmojiCategory.SMILEYS, listOf("astonished", "shocked", "amazed")),
    EmojiItem("😳", "flushed face", EmojiCategory.SMILEYS, listOf("flushed", "embarrassed", "blush")),
    EmojiItem("🥺", "pleading face", EmojiCategory.SMILEYS, listOf("pleading", "puppy", "eyes", "beg")),
    EmojiItem("😦", "frowning face with open mouth", EmojiCategory.SMILEYS, listOf("frown", "aww")),
    EmojiItem("😧", "anguished face", EmojiCategory.SMILEYS, listOf("anguished", "shocked")),
    EmojiItem("😨", "fearful face", EmojiCategory.SMILEYS, listOf("scared", "fear", "nervous")),
    EmojiItem("😰", "anxious face with sweat", EmojiCategory.SMILEYS, listOf("anxious", "sweat", "nervous")),
    EmojiItem("😥", "sad but relieved face", EmojiCategory.SMILEYS, listOf("relieved", "whew", "phew")),
    EmojiItem("😢", "crying face", EmojiCategory.SMILEYS, listOf("cry", "tear", "sad")),
    EmojiItem("😭", "loudly crying face", EmojiCategory.SMILEYS, listOf("sob", "cry", "tears", "sad")),
    EmojiItem("😱", "face screaming in fear", EmojiCategory.SMILEYS, listOf("scream", "horror", "shock")),
    EmojiItem("😖", "confounded face", EmojiCategory.SMILEYS, listOf("frustrated", "confounded")),
    EmojiItem("😣", "persevering face", EmojiCategory.SMILEYS, listOf("struggle", "persevere")),
    EmojiItem("😞", "disappointed face", EmojiCategory.SMILEYS, listOf("disappointed", "sad")),
    EmojiItem("😓", "downcast face with sweat", EmojiCategory.SMILEYS, listOf("sweat", "hard", "tired")),
    EmojiItem("😩", "weary face", EmojiCategory.SMILEYS, listOf("weary", "tired", "whine")),
    EmojiItem("😫", "tired face", EmojiCategory.SMILEYS, listOf("tired", "exhausted")),
    EmojiItem("🥱", "yawning face", EmojiCategory.SMILEYS, listOf("yawn", "sleepy", "bored")),
    EmojiItem("😤", "face with steam from nose", EmojiCategory.SMILEYS, listOf("steam", "proud", "frustrated")),
    EmojiItem("😡", "enraged face", EmojiCategory.SMILEYS, listOf("angry", "mad", "rage", "red")),
    EmojiItem("😠", "angry face", EmojiCategory.SMILEYS, listOf("angry", "mad", "annoyed")),
    EmojiItem("🤬", "face with symbols on mouth", EmojiCategory.SMILEYS, listOf("curse", "swear", "furious")),
    EmojiItem("😈", "smiling face with horns", EmojiCategory.SMILEYS, listOf("devil", "evil", "naughty")),
    EmojiItem("👿", "angry face with horns", EmojiCategory.SMILEYS, listOf("devil", "angry", "evil")),
    EmojiItem("💀", "skull", EmojiCategory.SMILEYS, listOf("skull", "dead", "skeleton")),
    EmojiItem("☠️", "skull and crossbones", EmojiCategory.SMILEYS, listOf("pirate", "danger", "poison")),
    EmojiItem("💩", "pile of poo", EmojiCategory.SMILEYS, listOf("poop", "poo", "crap")),
    EmojiItem("🤡", "clown face", EmojiCategory.SMILEYS, listOf("clown", "circus")),
    EmojiItem("👹", "ogre", EmojiCategory.SMILEYS, listOf("monster", "ogre", "japanese")),
    EmojiItem("👺", "goblin", EmojiCategory.SMILEYS, listOf("goblin", "red", "mask")),
    EmojiItem("👻", "ghost", EmojiCategory.SMILEYS, listOf("ghost", "halloween", "spooky")),
    EmojiItem("👽", "alien", EmojiCategory.SMILEYS, listOf("alien", "ufo", "space")),
    EmojiItem("👾", "alien monster", EmojiCategory.SMILEYS, listOf("game", "retro", "pixel")),
    EmojiItem("🤖", "robot", EmojiCategory.SMILEYS, listOf("robot", "bot", "ai")),
    EmojiItem("😺", "grinning cat", EmojiCategory.SMILEYS, listOf("cat", "smile")),
    EmojiItem("😸", "grinning cat with smiling eyes", EmojiCategory.SMILEYS, listOf("cat", "happy")),
    EmojiItem("😹", "cat with tears of joy", EmojiCategory.SMILEYS, listOf("cat", "laugh", "cry")),
    EmojiItem("😻", "smiling cat with heart-eyes", EmojiCategory.SMILEYS, listOf("cat", "love", "heart")),
    EmojiItem("😼", "cat with wry smile", EmojiCategory.SMILEYS, listOf("cat", "smirk")),
    EmojiItem("😽", "kissing cat", EmojiCategory.SMILEYS, listOf("cat", "kiss")),
    EmojiItem("🙀", "weary cat", EmojiCategory.SMILEYS, listOf("cat", "scream", "shock")),
    EmojiItem("😿", "crying cat", EmojiCategory.SMILEYS, listOf("cat", "tear", "sad")),
    EmojiItem("😾", "pouting cat", EmojiCategory.SMILEYS, listOf("cat", "angry")),
    EmojiItem("💋", "kiss mark", EmojiCategory.SMILEYS, listOf("kiss", "lips", "love")),
    EmojiItem("❤️", "red heart", EmojiCategory.SMILEYS, listOf("heart", "love", "red")),
    EmojiItem("🧡", "orange heart", EmojiCategory.SMILEYS, listOf("heart", "love", "orange")),
    EmojiItem("💛", "yellow heart", EmojiCategory.SMILEYS, listOf("heart", "love", "yellow")),
    EmojiItem("💚", "green heart", EmojiCategory.SMILEYS, listOf("heart", "love", "green")),
    EmojiItem("💙", "blue heart", EmojiCategory.SMILEYS, listOf("heart", "love", "blue")),
    EmojiItem("💜", "purple heart", EmojiCategory.SMILEYS, listOf("heart", "love", "purple")),
    EmojiItem("🖤", "black heart", EmojiCategory.SMILEYS, listOf("heart", "love", "black")),
    EmojiItem("🤍", "white heart", EmojiCategory.SMILEYS, listOf("heart", "love", "white")),
    EmojiItem("🤎", "brown heart", EmojiCategory.SMILEYS, listOf("heart", "love", "brown")),
    EmojiItem("💔", "broken heart", EmojiCategory.SMILEYS, listOf("broken", "heartbreak", "sad")),
    EmojiItem("❣️", "heart exclamation", EmojiCategory.SMILEYS, listOf("heart", "punctuation")),
    EmojiItem("💕", "two hearts", EmojiCategory.SMILEYS, listOf("love", "hearts", "pink")),
    EmojiItem("💞", "revolving hearts", EmojiCategory.SMILEYS, listOf("love", "spin", "hearts")),
    EmojiItem("💓", "beating heart", EmojiCategory.SMILEYS, listOf("heartbeat", "pulse")),
    EmojiItem("💗", "growing heart", EmojiCategory.SMILEYS, listOf("love", "grow", "hearts")),
    EmojiItem("💖", "sparkling heart", EmojiCategory.SMILEYS, listOf("love", "sparkle", "glitter")),
    EmojiItem("💘", "heart with arrow", EmojiCategory.SMILEYS, listOf("cupid", "love", "arrow")),
    EmojiItem("💝", "heart with ribbon", EmojiCategory.SMILEYS, listOf("gift", "love", "ribbon")),
    EmojiItem("🔥", "fire", EmojiCategory.SMILEYS, listOf("fire", "flame", "hot", "lit")),
    EmojiItem("✨", "sparkles", EmojiCategory.SMILEYS, listOf("sparkle", "shine", "stars", "magic")),
    EmojiItem("💯", "hundred points", EmojiCategory.SMILEYS, listOf("100", "score", "perfect")),
    )
  }

  private val people: List<EmojiItem> by lazy(LazyThreadSafetyMode.PUBLICATION) {
    listOf(
    EmojiItem("👋", "waving hand", EmojiCategory.PEOPLE, listOf("wave", "hello", "goodbye"), supportsSkinTone = true),
    EmojiItem("🤚", "raised back of hand", EmojiCategory.PEOPLE, listOf("hand", "back"), supportsSkinTone = true),
    EmojiItem("🖐️", "hand with fingers splayed", EmojiCategory.PEOPLE, listOf("hand", "five"), supportsSkinTone = true),
    EmojiItem("✋", "raised hand", EmojiCategory.PEOPLE, listOf("hand", "stop", "highfive"), supportsSkinTone = true),
    EmojiItem("🖖", "vulcan salute", EmojiCategory.PEOPLE, listOf("spock", "star trek", "live long"), supportsSkinTone = true),
    EmojiItem("👌", "OK hand", EmojiCategory.PEOPLE, listOf("ok", "good", "perfect"), supportsSkinTone = true),
    EmojiItem("🤏", "pinching hand", EmojiCategory.PEOPLE, listOf("pinch", "small", "little"), supportsSkinTone = true),
    EmojiItem("✌️", "victory hand", EmojiCategory.PEOPLE, listOf("peace", "victory", "two"), supportsSkinTone = true),
    EmojiItem("🤞", "crossed fingers", EmojiCategory.PEOPLE, listOf("luck", "hope", "promise"), supportsSkinTone = true),
    EmojiItem("🤟", "love-you gesture", EmojiCategory.PEOPLE, listOf("love", "ily", "rock"), supportsSkinTone = true),
    EmojiItem("🤘", "sign of the horns", EmojiCategory.PEOPLE, listOf("rock", "metal", "horns"), supportsSkinTone = true),
    EmojiItem("🤙", "call me hand", EmojiCategory.PEOPLE, listOf("call", "shaka", "hang loose"), supportsSkinTone = true),
    EmojiItem("👈", "backhand index pointing left", EmojiCategory.PEOPLE, listOf("point", "left"), supportsSkinTone = true),
    EmojiItem("👉", "backhand index pointing right", EmojiCategory.PEOPLE, listOf("point", "right"), supportsSkinTone = true),
    EmojiItem("👆", "backhand index pointing up", EmojiCategory.PEOPLE, listOf("point", "up"), supportsSkinTone = true),
    EmojiItem("🖕", "middle finger", EmojiCategory.PEOPLE, listOf("rude", "finger"), supportsSkinTone = true),
    EmojiItem("👇", "backhand index pointing down", EmojiCategory.PEOPLE, listOf("point", "down"), supportsSkinTone = true),
    EmojiItem("☝️", "index pointing up", EmojiCategory.PEOPLE, listOf("point", "one"), supportsSkinTone = true),
    EmojiItem("👍", "thumbs up", EmojiCategory.PEOPLE, listOf("yes", "like", "agree", "good"), supportsSkinTone = true),
    EmojiItem("👎", "thumbs down", EmojiCategory.PEOPLE, listOf("no", "dislike", "bad"), supportsSkinTone = true),
    EmojiItem("✊", "raised fist", EmojiCategory.PEOPLE, listOf("fist", "power", "strength"), supportsSkinTone = true),
    EmojiItem("👊", "oncoming fist", EmojiCategory.PEOPLE, listOf("punch", "fist bump"), supportsSkinTone = true),
    EmojiItem("🤛", "left-facing fist", EmojiCategory.PEOPLE, listOf("fist bump", "left"), supportsSkinTone = true),
    EmojiItem("🤜", "right-facing fist", EmojiCategory.PEOPLE, listOf("fist bump", "right"), supportsSkinTone = true),
    EmojiItem("👏", "clapping hands", EmojiCategory.PEOPLE, listOf("applause", "bravo", "clap"), supportsSkinTone = true),
    EmojiItem("🙌", "raising hands", EmojiCategory.PEOPLE, listOf("celebrate", "hurray", "praise"), supportsSkinTone = true),
    EmojiItem("👐", "open hands", EmojiCategory.PEOPLE, listOf("hands", "open"), supportsSkinTone = true),
    EmojiItem("🤲", "palms up together", EmojiCategory.PEOPLE, listOf("prayer", "dua", "hands"), supportsSkinTone = true),
    EmojiItem("🤝", "handshake", EmojiCategory.PEOPLE, listOf("deal", "agreement", "shake"), supportsSkinTone = true),
    EmojiItem("🙏", "folded hands", EmojiCategory.PEOPLE, listOf("please", "thank you", "pray", "namaste"), supportsSkinTone = true),
    EmojiItem("✍️", "writing hand", EmojiCategory.PEOPLE, listOf("write", "pen"), supportsSkinTone = true),
    EmojiItem("💅", "nail polish", EmojiCategory.PEOPLE, listOf("nails", "beauty", "sassy"), supportsSkinTone = true),
    EmojiItem("🤳", "selfie", EmojiCategory.PEOPLE, listOf("photo", "camera", "phone"), supportsSkinTone = true),
    EmojiItem("💪", "flexed biceps", EmojiCategory.PEOPLE, listOf("strong", "muscle", "workout"), supportsSkinTone = true),
    EmojiItem("👂", "ear", EmojiCategory.PEOPLE, listOf("hear", "listen"), supportsSkinTone = true),
    EmojiItem("👃", "nose", EmojiCategory.PEOPLE, listOf("smell", "sniff"), supportsSkinTone = true),
    EmojiItem("👀", "eyes", EmojiCategory.PEOPLE, listOf("look", "see", "watch")),
    EmojiItem("👁️", "eye", EmojiCategory.PEOPLE, listOf("see", "view")),
    EmojiItem("👅", "tongue", EmojiCategory.PEOPLE, listOf("taste", "lick")),
    EmojiItem("👄", "mouth", EmojiCategory.PEOPLE, listOf("lips", "kiss")),
    EmojiItem("👶", "baby", EmojiCategory.PEOPLE, listOf("child", "infant", "toddler"), supportsSkinTone = true),
    EmojiItem("🧒", "child", EmojiCategory.PEOPLE, listOf("kid", "young"), supportsSkinTone = true),
    EmojiItem("👦", "boy", EmojiCategory.PEOPLE, listOf("kid", "male"), supportsSkinTone = true),
    EmojiItem("👧", "girl", EmojiCategory.PEOPLE, listOf("kid", "female"), supportsSkinTone = true),
    EmojiItem("🧑", "person", EmojiCategory.PEOPLE, listOf("human", "adult"), supportsSkinTone = true),
    EmojiItem("👨", "man", EmojiCategory.PEOPLE, listOf("male", "guy"), supportsSkinTone = true),
    EmojiItem("👩", "woman", EmojiCategory.PEOPLE, listOf("female", "lady"), supportsSkinTone = true),
    EmojiItem("🧓", "older person", EmojiCategory.PEOPLE, listOf("elder", "senior"), supportsSkinTone = true),
    EmojiItem("👴", "old man", EmojiCategory.PEOPLE, listOf("grandfather", "elder"), supportsSkinTone = true),
    EmojiItem("👵", "old woman", EmojiCategory.PEOPLE, listOf("grandmother", "elder"), supportsSkinTone = true),
    EmojiItem("🙋", "person raising hand", EmojiCategory.PEOPLE, listOf("question", "volunteer"), supportsSkinTone = true),
    EmojiItem("🙇", "person bowing", EmojiCategory.PEOPLE, listOf("sorry", "respect"), supportsSkinTone = true),
    EmojiItem("🤦", "person facepalming", EmojiCategory.PEOPLE, listOf("oops", "disbelief"), supportsSkinTone = true),
    EmojiItem("🤷", "person shrugging", EmojiCategory.PEOPLE, listOf("idk", "shrug", "doubt"), supportsSkinTone = true),
    EmojiItem("🧑‍💻", "technologist", EmojiCategory.PEOPLE, listOf("code", "developer", "computer"), supportsSkinTone = true),
    EmojiItem("👨‍💻", "man technologist", EmojiCategory.PEOPLE, listOf("coder", "programmer"), supportsSkinTone = true),
    EmojiItem("👩‍💻", "woman technologist", EmojiCategory.PEOPLE, listOf("coder", "programmer"), supportsSkinTone = true),
    EmojiItem("👮", "police officer", EmojiCategory.PEOPLE, listOf("cop", "law"), supportsSkinTone = true),
    EmojiItem("👷", "construction worker", EmojiCategory.PEOPLE, listOf("builder", "helmet"), supportsSkinTone = true),
    EmojiItem("🤴", "prince", EmojiCategory.PEOPLE, listOf("royalty", "crown"), supportsSkinTone = true),
    EmojiItem("👸", "princess", EmojiCategory.PEOPLE, listOf("royalty", "crown"), supportsSkinTone = true),
    EmojiItem("🚶", "person walking", EmojiCategory.PEOPLE, listOf("walk", "stroll"), supportsSkinTone = true),
    EmojiItem("🏃", "person running", EmojiCategory.PEOPLE, listOf("run", "sprint"), supportsSkinTone = true),
    EmojiItem("💃", "woman dancing", EmojiCategory.PEOPLE, listOf("dance", "party", "salsa"), supportsSkinTone = true),
    EmojiItem("🕺", "man dancing", EmojiCategory.PEOPLE, listOf("dance", "disco"), supportsSkinTone = true),
    EmojiItem("👨‍👩‍👧‍👦", "family (man, woman, girl, boy)", EmojiCategory.PEOPLE, listOf("family", "parents", "children")),
    EmojiItem("👨‍👩‍👧", "family (man, woman, girl)", EmojiCategory.PEOPLE, listOf("family", "daughter")),
    EmojiItem("👨‍👩‍👦", "family (man, woman, boy)", EmojiCategory.PEOPLE, listOf("family", "son")),
    EmojiItem("🧑‍🤝‍🧑", "people holding hands", EmojiCategory.PEOPLE, listOf("couple", "friends", "together")),
    )
  }

  private val animals: List<EmojiItem> by lazy(LazyThreadSafetyMode.PUBLICATION) {
    listOf(
    EmojiItem("🐶", "dog face", EmojiCategory.ANIMALS, listOf("dog", "puppy", "pet")),
    EmojiItem("🐱", "cat face", EmojiCategory.ANIMALS, listOf("cat", "kitten", "pet")),
    EmojiItem("🐭", "mouse face", EmojiCategory.ANIMALS, listOf("mouse", "rodent")),
    EmojiItem("🐹", "hamster", EmojiCategory.ANIMALS, listOf("hamster", "pet")),
    EmojiItem("🐰", "rabbit face", EmojiCategory.ANIMALS, listOf("bunny", "rabbit", "easter")),
    EmojiItem("🦊", "fox", EmojiCategory.ANIMALS, listOf("fox", "cunning")),
    EmojiItem("🐻", "bear", EmojiCategory.ANIMALS, listOf("bear", "grizzly")),
    EmojiItem("🐼", "panda", EmojiCategory.ANIMALS, listOf("panda", "bear", "china")),
    EmojiItem("🐨", "koala", EmojiCategory.ANIMALS, listOf("koala", "australia")),
    EmojiItem("🐯", "tiger face", EmojiCategory.ANIMALS, listOf("tiger", "cat", "wild")),
    EmojiItem("🦁", "lion", EmojiCategory.ANIMALS, listOf("lion", "king", "roar")),
    EmojiItem("🐮", "cow face", EmojiCategory.ANIMALS, listOf("cow", "cattle", "milk")),
    EmojiItem("🐷", "pig face", EmojiCategory.ANIMALS, listOf("pig", "oink")),
    EmojiItem("🐸", "frog", EmojiCategory.ANIMALS, listOf("frog", "toad")),
    EmojiItem("🐵", "monkey face", EmojiCategory.ANIMALS, listOf("monkey", "ape")),
    EmojiItem("🐔", "chicken", EmojiCategory.ANIMALS, listOf("chicken", "hen", "bird")),
    EmojiItem("🐧", "penguin", EmojiCategory.ANIMALS, listOf("penguin", "ice", "bird")),
    EmojiItem("🐦", "bird", EmojiCategory.ANIMALS, listOf("bird", "fly")),
    EmojiItem("🐤", "baby chick", EmojiCategory.ANIMALS, listOf("chick", "cute")),
    EmojiItem("🦆", "duck", EmojiCategory.ANIMALS, listOf("duck", "bird")),
    EmojiItem("🦅", "eagle", EmojiCategory.ANIMALS, listOf("eagle", "bird", "america")),
    EmojiItem("🦉", "owl", EmojiCategory.ANIMALS, listOf("owl", "wise", "night")),
    EmojiItem("🦇", "bat", EmojiCategory.ANIMALS, listOf("bat", "vampire", "halloween")),
    EmojiItem("🐺", "wolf", EmojiCategory.ANIMALS, listOf("wolf", "howl")),
    EmojiItem("🐗", "boar", EmojiCategory.ANIMALS, listOf("boar", "pig")),
    EmojiItem("🐴", "horse face", EmojiCategory.ANIMALS, listOf("horse", "pony")),
    EmojiItem("🦄", "unicorn", EmojiCategory.ANIMALS, listOf("unicorn", "magic", "fantasy")),
    EmojiItem("🐝", "honeybee", EmojiCategory.ANIMALS, listOf("bee", "honey", "insect")),
    EmojiItem("🐛", "bug", EmojiCategory.ANIMALS, listOf("bug", "insect", "caterpillar")),
    EmojiItem("🦋", "butterfly", EmojiCategory.ANIMALS, listOf("butterfly", "insect", "pretty")),
    EmojiItem("🐌", "snail", EmojiCategory.ANIMALS, listOf("snail", "slow")),
    EmojiItem("🐞", "lady beetle", EmojiCategory.ANIMALS, listOf("ladybug", "insect")),
    EmojiItem("🐜", "ant", EmojiCategory.ANIMALS, listOf("ant", "insect")),
    EmojiItem("🕷️", "spider", EmojiCategory.ANIMALS, listOf("spider", "web")),
    EmojiItem("🦂", "scorpion", EmojiCategory.ANIMALS, listOf("scorpion")),
    EmojiItem("🦟", "mosquito", EmojiCategory.ANIMALS, listOf("mosquito", "bite")),
    EmojiItem("🐢", "turtle", EmojiCategory.ANIMALS, listOf("turtle", "slow")),
    EmojiItem("🐍", "snake", EmojiCategory.ANIMALS, listOf("snake", "reptile")),
    EmojiItem("🦎", "lizard", EmojiCategory.ANIMALS, listOf("lizard", "reptile")),
    EmojiItem("🐙", "octopus", EmojiCategory.ANIMALS, listOf("octopus", "sea")),
    EmojiItem("🦑", "squid", EmojiCategory.ANIMALS, listOf("squid", "sea")),
    EmojiItem("🦐", "shrimp", EmojiCategory.ANIMALS, listOf("shrimp", "seafood")),
    EmojiItem("🦞", "lobster", EmojiCategory.ANIMALS, listOf("lobster", "seafood")),
    EmojiItem("🦀", "crab", EmojiCategory.ANIMALS, listOf("crab", "seafood")),
    EmojiItem("🐡", "blowfish", EmojiCategory.ANIMALS, listOf("fish", "puffer")),
    EmojiItem("🐠", "tropical fish", EmojiCategory.ANIMALS, listOf("fish", "tropical")),
    EmojiItem("🐟", "fish", EmojiCategory.ANIMALS, listOf("fish", "sea")),
    EmojiItem("🐬", "dolphin", EmojiCategory.ANIMALS, listOf("dolphin", "sea")),
    EmojiItem("🐳", "spouting whale", EmojiCategory.ANIMALS, listOf("whale", "sea")),
    EmojiItem("🦈", "shark", EmojiCategory.ANIMALS, listOf("shark", "sea", "predator")),
    EmojiItem("🐊", "crocodile", EmojiCategory.ANIMALS, listOf("crocodile", "alligator")),
    EmojiItem("🐅", "tiger", EmojiCategory.ANIMALS, listOf("tiger", "animal")),
    EmojiItem("🐆", "leopard", EmojiCategory.ANIMALS, listOf("leopard", "cheetah")),
    EmojiItem("🦓", "zebra", EmojiCategory.ANIMALS, listOf("zebra", "stripes")),
    EmojiItem("🦍", "gorilla", EmojiCategory.ANIMALS, listOf("gorilla", "ape")),
    EmojiItem("🦧", "orangutan", EmojiCategory.ANIMALS, listOf("orangutan", "ape")),
    EmojiItem("🐘", "elephant", EmojiCategory.ANIMALS, listOf("elephant", "trunk")),
    EmojiItem("🦛", "hippopotamus", EmojiCategory.ANIMALS, listOf("hippo")),
    EmojiItem("🦏", "rhinoceros", EmojiCategory.ANIMALS, listOf("rhino")),
    EmojiItem("🐪", "camel", EmojiCategory.ANIMALS, listOf("camel", "desert")),
    EmojiItem("🦒", "giraffe", EmojiCategory.ANIMALS, listOf("giraffe", "tall")),
    EmojiItem("🦘", "kangaroo", EmojiCategory.ANIMALS, listOf("kangaroo", "australia")),
    EmojiItem("🌸", "cherry blossom", EmojiCategory.ANIMALS, listOf("flower", "sakura", "spring")),
    EmojiItem("🌹", "rose", EmojiCategory.ANIMALS, listOf("flower", "rose", "red", "love")),
    EmojiItem("🌺", "hibiscus", EmojiCategory.ANIMALS, listOf("flower", "tropical")),
    EmojiItem("🌻", "sunflower", EmojiCategory.ANIMALS, listOf("flower", "sun", "yellow")),
    EmojiItem("🌼", "blossom", EmojiCategory.ANIMALS, listOf("flower", "yellow")),
    EmojiItem("🌷", "tulip", EmojiCategory.ANIMALS, listOf("flower", "tulip")),
    EmojiItem("🌱", "seedling", EmojiCategory.ANIMALS, listOf("plant", "grow", "sprout")),
    EmojiItem("🌲", "evergreen tree", EmojiCategory.ANIMALS, listOf("tree", "pine")),
    EmojiItem("🌳", "deciduous tree", EmojiCategory.ANIMALS, listOf("tree", "green")),
    EmojiItem("🌴", "palm tree", EmojiCategory.ANIMALS, listOf("tree", "beach", "tropical")),
    EmojiItem("🌵", "cactus", EmojiCategory.ANIMALS, listOf("desert", "plant")),
    EmojiItem("🍀", "four leaf clover", EmojiCategory.ANIMALS, listOf("clover", "luck", "irish")),
    EmojiItem("🍁", "maple leaf", EmojiCategory.ANIMALS, listOf("leaf", "autumn", "canada")),
    EmojiItem("🍂", "fallen leaf", EmojiCategory.ANIMALS, listOf("leaf", "fall", "autumn")),
    )
  }

  private val food: List<EmojiItem> by lazy(LazyThreadSafetyMode.PUBLICATION) {
    listOf(
    EmojiItem("🍏", "green apple", EmojiCategory.FOOD, listOf("apple", "fruit")),
    EmojiItem("🍎", "red apple", EmojiCategory.FOOD, listOf("apple", "fruit", "red")),
    EmojiItem("🍐", "pear", EmojiCategory.FOOD, listOf("pear", "fruit")),
    EmojiItem("🍊", "tangerine", EmojiCategory.FOOD, listOf("orange", "citrus")),
    EmojiItem("🍋", "lemon", EmojiCategory.FOOD, listOf("lemon", "sour")),
    EmojiItem("🍌", "banana", EmojiCategory.FOOD, listOf("banana", "fruit")),
    EmojiItem("🍉", "watermelon", EmojiCategory.FOOD, listOf("watermelon", "fruit", "summer")),
    EmojiItem("🍇", "grapes", EmojiCategory.FOOD, listOf("grapes", "fruit", "wine")),
    EmojiItem("🍓", "strawberry", EmojiCategory.FOOD, listOf("strawberry", "fruit", "berry")),
    EmojiItem("🫐", "blueberries", EmojiCategory.FOOD, listOf("blueberry", "berry")),
    EmojiItem("🍈", "melon", EmojiCategory.FOOD, listOf("melon", "fruit")),
    EmojiItem("🍒", "cherries", EmojiCategory.FOOD, listOf("cherry", "fruit")),
    EmojiItem("🍑", "peach", EmojiCategory.FOOD, listOf("peach", "fruit")),
    EmojiItem("🥭", "mango", EmojiCategory.FOOD, listOf("mango", "fruit")),
    EmojiItem("🍍", "pineapple", EmojiCategory.FOOD, listOf("pineapple", "fruit")),
    EmojiItem("🥥", "coconut", EmojiCategory.FOOD, listOf("coconut", "tropical")),
    EmojiItem("🥝", "kiwi fruit", EmojiCategory.FOOD, listOf("kiwi", "fruit")),
    EmojiItem("🍅", "tomato", EmojiCategory.FOOD, listOf("tomato", "vegetable")),
    EmojiItem("🥑", "avocado", EmojiCategory.FOOD, listOf("avocado", "guacamole")),
    EmojiItem("🥦", "broccoli", EmojiCategory.FOOD, listOf("broccoli", "vegetable", "green")),
    EmojiItem("🥕", "carrot", EmojiCategory.FOOD, listOf("carrot", "vegetable")),
    EmojiItem("🌽", "ear of corn", EmojiCategory.FOOD, listOf("corn", "maize")),
    EmojiItem("🌶️", "hot pepper", EmojiCategory.FOOD, listOf("pepper", "spicy", "chili")),
    EmojiItem("🥒", "cucumber", EmojiCategory.FOOD, listOf("cucumber", "pickle")),
    EmojiItem("🥔", "potato", EmojiCategory.FOOD, listOf("potato", "spud")),
    EmojiItem("🍞", "bread", EmojiCategory.FOOD, listOf("bread", "toast", "bakery")),
    EmojiItem("🥐", "croissant", EmojiCategory.FOOD, listOf("croissant", "pastry")),
    EmojiItem("🥖", "baguette bread", EmojiCategory.FOOD, listOf("baguette", "french")),
    EmojiItem("🧀", "cheese wedge", EmojiCategory.FOOD, listOf("cheese", "dairy")),
    EmojiItem("🍗", "poultry leg", EmojiCategory.FOOD, listOf("chicken", "meat", "drumstick")),
    EmojiItem("🥩", "cut of meat", EmojiCategory.FOOD, listOf("meat", "steak", "beef")),
    EmojiItem("🍔", "hamburger", EmojiCategory.FOOD, listOf("burger", "fast food")),
    EmojiItem("🍟", "french fries", EmojiCategory.FOOD, listOf("fries", "fast food")),
    EmojiItem("🍕", "pizza", EmojiCategory.FOOD, listOf("pizza", "cheese", "slice")),
    EmojiItem("🌭", "hot dog", EmojiCategory.FOOD, listOf("hotdog", "sausage")),
    EmojiItem("🥪", "sandwich", EmojiCategory.FOOD, listOf("sandwich", "lunch")),
    EmojiItem("🌮", "taco", EmojiCategory.FOOD, listOf("taco", "mexican")),
    EmojiItem("🌯", "burrito", EmojiCategory.FOOD, listOf("burrito", "mexican")),
    EmojiItem("🥚", "egg", EmojiCategory.FOOD, listOf("egg", "breakfast")),
    EmojiItem("🍳", "cooking", EmojiCategory.FOOD, listOf("egg", "fry", "pan")),
    EmojiItem("🍿", "popcorn", EmojiCategory.FOOD, listOf("popcorn", "movie", "snack")),
    EmojiItem("🥗", "green salad", EmojiCategory.FOOD, listOf("salad", "healthy")),
    EmojiItem("🍝", "spaghetti", EmojiCategory.FOOD, listOf("pasta", "italian", "noodles")),
    EmojiItem("🍜", "steaming bowl", EmojiCategory.FOOD, listOf("ramen", "soup", "noodles")),
    EmojiItem("🍲", "pot of food", EmojiCategory.FOOD, listOf("stew", "soup")),
    EmojiItem("🍛", "curry rice", EmojiCategory.FOOD, listOf("curry", "rice", "indian", "bangla")),
    EmojiItem("🍚", "cooked rice", EmojiCategory.FOOD, listOf("rice", "bowl")),
    EmojiItem("🍣", "sushi", EmojiCategory.FOOD, listOf("sushi", "japanese")),
    EmojiItem("🍤", "fried shrimp", EmojiCategory.FOOD, listOf("shrimp", "tempura")),
    EmojiItem("🍦", "soft ice cream", EmojiCategory.FOOD, listOf("ice cream", "dessert")),
    EmojiItem("🍧", "shaved ice", EmojiCategory.FOOD, listOf("shaved ice", "sweet")),
    EmojiItem("🍨", "ice cream", EmojiCategory.FOOD, listOf("ice cream", "dessert", "bowl")),
    EmojiItem("🍩", "doughnut", EmojiCategory.FOOD, listOf("donut", "sweet")),
    EmojiItem("🍪", "cookie", EmojiCategory.FOOD, listOf("cookie", "biscuit", "chocolate")),
    EmojiItem("🎂", "birthday cake", EmojiCategory.FOOD, listOf("cake", "birthday", "party")),
    EmojiItem("🍰", "shortcake", EmojiCategory.FOOD, listOf("cake", "slice")),
    EmojiItem("🧁", "cupcake", EmojiCategory.FOOD, listOf("cupcake", "dessert")),
    EmojiItem("🍫", "chocolate bar", EmojiCategory.FOOD, listOf("chocolate", "candy")),
    EmojiItem("🍬", "candy", EmojiCategory.FOOD, listOf("candy", "sweet")),
    EmojiItem("🍭", "lollipop", EmojiCategory.FOOD, listOf("lollipop", "candy")),
    EmojiItem("☕", "hot beverage", EmojiCategory.FOOD, listOf("coffee", "tea", "cafe")),
    EmojiItem("🫖", "teapot", EmojiCategory.FOOD, listOf("tea", "pot")),
    EmojiItem("🍵", "teacup without handle", EmojiCategory.FOOD, listOf("tea", "green tea", "matcha")),
    EmojiItem("🥛", "glass of milk", EmojiCategory.FOOD, listOf("milk", "dairy")),
    EmojiItem("🧃", "beverage box", EmojiCategory.FOOD, listOf("juice", "drink")),
    EmojiItem("🥤", "cup with straw", EmojiCategory.FOOD, listOf("soda", "drink")),
    EmojiItem("🧋", "bubble tea", EmojiCategory.FOOD, listOf("boba", "bubble tea")),
    )
  }

  private val travel: List<EmojiItem> by lazy(LazyThreadSafetyMode.PUBLICATION) {
    listOf(
    EmojiItem("🚗", "automobile", EmojiCategory.TRAVEL, listOf("car", "drive")),
    EmojiItem("🚕", "taxi", EmojiCategory.TRAVEL, listOf("cab", "taxi")),
    EmojiItem("🚙", "sport utility vehicle", EmojiCategory.TRAVEL, listOf("suv", "car")),
    EmojiItem("🚌", "bus", EmojiCategory.TRAVEL, listOf("bus", "transit")),
    EmojiItem("🚎", "trolleybus", EmojiCategory.TRAVEL, listOf("trolley", "bus")),
    EmojiItem("🏎️", "racing car", EmojiCategory.TRAVEL, listOf("race", "fast", "f1")),
    EmojiItem("🚓", "police car", EmojiCategory.TRAVEL, listOf("police", "cop")),
    EmojiItem("🚑", "ambulance", EmojiCategory.TRAVEL, listOf("ambulance", "hospital")),
    EmojiItem("🚒", "fire engine", EmojiCategory.TRAVEL, listOf("fire", "truck")),
    EmojiItem("🚚", "delivery truck", EmojiCategory.TRAVEL, listOf("truck", "delivery")),
    EmojiItem("🏍️", "motorcycle", EmojiCategory.TRAVEL, listOf("bike", "motorcycle")),
    EmojiItem("🛵", "motor scooter", EmojiCategory.TRAVEL, listOf("scooter", "vespa")),
    EmojiItem("🚲", "bicycle", EmojiCategory.TRAVEL, listOf("bike", "cycle")),
    EmojiItem("🛴", "kick scooter", EmojiCategory.TRAVEL, listOf("scooter")),
    EmojiItem("🚆", "train", EmojiCategory.TRAVEL, listOf("train", "railway")),
    EmojiItem("🚇", "metro", EmojiCategory.TRAVEL, listOf("metro", "subway")),
    EmojiItem("🚅", "bullet train", EmojiCategory.TRAVEL, listOf("shinkansen", "fast train")),
    EmojiItem("✈️", "airplane", EmojiCategory.TRAVEL, listOf("plane", "flight", "fly")),
    EmojiItem("🛫", "airplane departure", EmojiCategory.TRAVEL, listOf("departure", "takeoff")),
    EmojiItem("🛬", "airplane arrival", EmojiCategory.TRAVEL, listOf("arrival", "landing")),
    EmojiItem("🚀", "rocket", EmojiCategory.TRAVEL, listOf("space", "launch", "moon")),
    EmojiItem("🚁", "helicopter", EmojiCategory.TRAVEL, listOf("helicopter", "chopper")),
    EmojiItem("🛸", "flying saucer", EmojiCategory.TRAVEL, listOf("ufo", "alien")),
    EmojiItem("⛵", "sailboat", EmojiCategory.TRAVEL, listOf("boat", "sail", "sea")),
    EmojiItem("🚤", "speedboat", EmojiCategory.TRAVEL, listOf("boat", "motorboat")),
    EmojiItem("🛳️", "passenger ship", EmojiCategory.TRAVEL, listOf("cruise", "ship")),
    EmojiItem("⚓", "anchor", EmojiCategory.TRAVEL, listOf("ship", "navy")),
    EmojiItem("⛽", "fuel pump", EmojiCategory.TRAVEL, listOf("gas", "petrol")),
    EmojiItem("🗺️", "world map", EmojiCategory.TRAVEL, listOf("map", "travel", "world")),
    EmojiItem("🗿", "moai", EmojiCategory.TRAVEL, listOf("statue", "easter island")),
    EmojiItem("🗽", "Statue of Liberty", EmojiCategory.TRAVEL, listOf("new york", "usa")),
    EmojiItem("🗼", "Tokyo tower", EmojiCategory.TRAVEL, listOf("tokyo", "japan")),
    EmojiItem("🏰", "castle", EmojiCategory.TRAVEL, listOf("castle", "disney")),
    EmojiItem("🏠", "house", EmojiCategory.TRAVEL, listOf("home", "building")),
    EmojiItem("🏡", "house with garden", EmojiCategory.TRAVEL, listOf("home", "yard")),
    EmojiItem("🏢", "office building", EmojiCategory.TRAVEL, listOf("office", "work")),
    EmojiItem("🏥", "hospital", EmojiCategory.TRAVEL, listOf("hospital", "doctor")),
    EmojiItem("🏦", "bank", EmojiCategory.TRAVEL, listOf("bank", "money")),
    EmojiItem("🏨", "hotel", EmojiCategory.TRAVEL, listOf("hotel", "resort")),
    EmojiItem("🏫", "school", EmojiCategory.TRAVEL, listOf("school", "education")),
    EmojiItem("🕌", "mosque", EmojiCategory.TRAVEL, listOf("mosque", "islam", "prayer")),
    EmojiItem("🕋", "kaaba", EmojiCategory.TRAVEL, listOf("mecca", "hajj", "islam")),
    EmojiItem("⛰️", "mountain", EmojiCategory.TRAVEL, listOf("mountain", "hike")),
    EmojiItem("🏖️", "beach with umbrella", EmojiCategory.TRAVEL, listOf("beach", "vacation")),
    EmojiItem("🏜️", "desert", EmojiCategory.TRAVEL, listOf("desert", "sand")),
    EmojiItem("🌋", "volcano", EmojiCategory.TRAVEL, listOf("volcano", "lava")),
    )
  }

  private val activities: List<EmojiItem> by lazy(LazyThreadSafetyMode.PUBLICATION) {
    listOf(
    EmojiItem("⚽", "soccer ball", EmojiCategory.ACTIVITIES, listOf("soccer", "football")),
    EmojiItem("🏀", "basketball", EmojiCategory.ACTIVITIES, listOf("basketball", "hoop")),
    EmojiItem("🏈", "american football", EmojiCategory.ACTIVITIES, listOf("football", "nfl")),
    EmojiItem("⚾", "baseball", EmojiCategory.ACTIVITIES, listOf("baseball")),
    EmojiItem("🥎", "softball", EmojiCategory.ACTIVITIES, listOf("softball")),
    EmojiItem("🎾", "tennis", EmojiCategory.ACTIVITIES, listOf("tennis", "racket")),
    EmojiItem("🏐", "volleyball", EmojiCategory.ACTIVITIES, listOf("volleyball")),
    EmojiItem("🏉", "rugby football", EmojiCategory.ACTIVITIES, listOf("rugby")),
    EmojiItem("🥏", "flying disc", EmojiCategory.ACTIVITIES, listOf("frisbee")),
    EmojiItem("🎱", "pool 8 ball", EmojiCategory.ACTIVITIES, listOf("billiards", "8 ball")),
    EmojiItem("🏓", "ping pong", EmojiCategory.ACTIVITIES, listOf("table tennis", "pingpong")),
    EmojiItem("🏸", "badminton", EmojiCategory.ACTIVITIES, listOf("badminton")),
    EmojiItem("🏏", "cricket game", EmojiCategory.ACTIVITIES, listOf("cricket", "bat", "ball")),
    EmojiItem("🥊", "boxing glove", EmojiCategory.ACTIVITIES, listOf("boxing", "fight")),
    EmojiItem("🥋", "martial arts uniform", EmojiCategory.ACTIVITIES, listOf("karate", "judo")),
    EmojiItem("🛹", "skateboard", EmojiCategory.ACTIVITIES, listOf("skate")),
    EmojiItem("🏋️", "person lifting weights", EmojiCategory.ACTIVITIES, listOf("gym", "lift", "workout")),
    EmojiItem("🚴", "person biking", EmojiCategory.ACTIVITIES, listOf("bicycle", "bike")),
    EmojiItem("🏊", "person swimming", EmojiCategory.ACTIVITIES, listOf("swim", "pool")),
    EmojiItem("🧗", "person climbing", EmojiCategory.ACTIVITIES, listOf("climb", "rock")),
    EmojiItem("🏆", "trophy", EmojiCategory.ACTIVITIES, listOf("award", "winner", "first")),
    EmojiItem("🥇", "1st place medal", EmojiCategory.ACTIVITIES, listOf("gold", "first", "winner")),
    EmojiItem("🥈", "2nd place medal", EmojiCategory.ACTIVITIES, listOf("silver", "second")),
    EmojiItem("🥉", "3rd place medal", EmojiCategory.ACTIVITIES, listOf("bronze", "third")),
    EmojiItem("🏅", "sports medal", EmojiCategory.ACTIVITIES, listOf("medal", "award")),
    EmojiItem("🎫", "ticket", EmojiCategory.ACTIVITIES, listOf("ticket", "pass", "entry")),
    EmojiItem("🎪", "circus tent", EmojiCategory.ACTIVITIES, listOf("circus", "tent")),
    EmojiItem("🎨", "artist palette", EmojiCategory.ACTIVITIES, listOf("art", "paint", "draw")),
    EmojiItem("🎬", "clapper board", EmojiCategory.ACTIVITIES, listOf("movie", "cinema", "film")),
    EmojiItem("🎤", "microphone", EmojiCategory.ACTIVITIES, listOf("mic", "sing", "karaoke")),
    EmojiItem("🎧", "headphone", EmojiCategory.ACTIVITIES, listOf("music", "listen")),
    EmojiItem("🎼", "musical score", EmojiCategory.ACTIVITIES, listOf("music", "notes")),
    EmojiItem("🎹", "musical keyboard", EmojiCategory.ACTIVITIES, listOf("piano", "music")),
    EmojiItem("🥁", "drum", EmojiCategory.ACTIVITIES, listOf("drum", "music")),
    EmojiItem("🎷", "saxophone", EmojiCategory.ACTIVITIES, listOf("sax", "jazz")),
    EmojiItem("🎺", "trumpet", EmojiCategory.ACTIVITIES, listOf("trumpet", "brass")),
    EmojiItem("🎸", "guitar", EmojiCategory.ACTIVITIES, listOf("guitar", "rock")),
    EmojiItem("🎮", "video game", EmojiCategory.ACTIVITIES, listOf("game", "controller", "gaming")),
    EmojiItem("🎯", "bullseye", EmojiCategory.ACTIVITIES, listOf("target", "dart")),
    EmojiItem("🎲", "game die", EmojiCategory.ACTIVITIES, listOf("dice", "board game")),
    EmojiItem("♟️", "chess pawn", EmojiCategory.ACTIVITIES, listOf("chess", "game")),
    )
  }

  private val objects: List<EmojiItem> by lazy(LazyThreadSafetyMode.PUBLICATION) {
    listOf(
    EmojiItem("📱", "mobile phone", EmojiCategory.OBJECTS, listOf("phone", "smartphone", "iphone")),
    EmojiItem("💻", "laptop", EmojiCategory.OBJECTS, listOf("computer", "pc", "mac")),
    EmojiItem("🖥️", "desktop computer", EmojiCategory.OBJECTS, listOf("screen", "monitor")),
    EmojiItem("⌨️", "keyboard", EmojiCategory.OBJECTS, listOf("type", "keyboard")),
    EmojiItem("🖱️", "computer mouse", EmojiCategory.OBJECTS, listOf("mouse", "click")),
    EmojiItem("📷", "camera", EmojiCategory.OBJECTS, listOf("photo", "picture")),
    EmojiItem("📸", "camera with flash", EmojiCategory.OBJECTS, listOf("photo", "flash")),
    EmojiItem("📹", "video camera", EmojiCategory.OBJECTS, listOf("video", "movie")),
    EmojiItem("📺", "television", EmojiCategory.OBJECTS, listOf("tv", "screen")),
    EmojiItem("📻", "radio", EmojiCategory.OBJECTS, listOf("radio", "broadcast")),
    EmojiItem("🔋", "battery", EmojiCategory.OBJECTS, listOf("power", "charge")),
    EmojiItem("🔌", "electric plug", EmojiCategory.OBJECTS, listOf("cord", "power")),
    EmojiItem("💡", "light bulb", EmojiCategory.OBJECTS, listOf("idea", "light", "lamp")),
    EmojiItem("🔦", "flashlight", EmojiCategory.OBJECTS, listOf("torch", "light")),
    EmojiItem("🕯️", "candle", EmojiCategory.OBJECTS, listOf("candle", "flame")),
    EmojiItem("💣", "bomb", EmojiCategory.OBJECTS, listOf("explosive", "boom")),
    EmojiItem("🔪", "kitchen knife", EmojiCategory.OBJECTS, listOf("knife", "blade")),
    EmojiItem("🗡️", "dagger", EmojiCategory.OBJECTS, listOf("sword", "blade")),
    EmojiItem("🛡️", "shield", EmojiCategory.OBJECTS, listOf("protect", "defense")),
    EmojiItem("🔑", "key", EmojiCategory.OBJECTS, listOf("lock", "password")),
    EmojiItem("🔒", "locked", EmojiCategory.OBJECTS, listOf("lock", "secure")),
    EmojiItem("🔓", "unlocked", EmojiCategory.OBJECTS, listOf("open", "insecure")),
    EmojiItem("🔔", "bell", EmojiCategory.OBJECTS, listOf("notification", "ring")),
    EmojiItem("🔕", "bell with slash", EmojiCategory.OBJECTS, listOf("mute", "silent")),
    EmojiItem("📦", "package", EmojiCategory.OBJECTS, listOf("box", "delivery")),
    EmojiItem("✉️", "envelope", EmojiCategory.OBJECTS, listOf("mail", "letter", "email")),
    EmojiItem("📧", "e-mail", EmojiCategory.OBJECTS, listOf("mail", "email")),
    EmojiItem("📝", "memo", EmojiCategory.OBJECTS, listOf("note", "write", "pad")),
    EmojiItem("✏️", "pencil", EmojiCategory.OBJECTS, listOf("write", "draw")),
    EmojiItem("🖊️", "pen", EmojiCategory.OBJECTS, listOf("ballpoint", "write")),
    EmojiItem("📚", "books", EmojiCategory.OBJECTS, listOf("read", "study", "library")),
    EmojiItem("📖", "open book", EmojiCategory.OBJECTS, listOf("read", "book")),
    EmojiItem("📄", "page facing up", EmojiCategory.OBJECTS, listOf("document", "file")),
    EmojiItem("📅", "calendar", EmojiCategory.OBJECTS, listOf("date", "schedule")),
    EmojiItem("💰", "money bag", EmojiCategory.OBJECTS, listOf("money", "dollar", "cash")),
    EmojiItem("💵", "dollar banknote", EmojiCategory.OBJECTS, listOf("money", "cash")),
    EmojiItem("💳", "credit card", EmojiCategory.OBJECTS, listOf("payment", "card")),
    EmojiItem("💎", "gem stone", EmojiCategory.OBJECTS, listOf("diamond", "jewel")),
    EmojiItem("🔍", "magnifying glass tilted left", EmojiCategory.OBJECTS, listOf("search", "find")),
    EmojiItem("🎁", "wrapped gift", EmojiCategory.OBJECTS, listOf("present", "birthday")),
    )
  }

  private val symbols: List<EmojiItem> by lazy(LazyThreadSafetyMode.PUBLICATION) {
    listOf(
    EmojiItem("♻️", "recycling symbol", EmojiCategory.SYMBOLS, listOf("recycle", "green", "environment")),
    EmojiItem("🔱", "trident emblem", EmojiCategory.SYMBOLS, listOf("trident", "spear", "poseidon")),
    EmojiItem("⚜️", "fleur-de-lis", EmojiCategory.SYMBOLS, listOf("fleur", "royal", "scout")),
    EmojiItem("🔰", "Japanese symbol for beginner", EmojiCategory.SYMBOLS, listOf("beginner", "chevron", "shield")),
    EmojiItem("⭕", "hollow red circle", EmojiCategory.SYMBOLS, listOf("circle", "correct", "ring")),
    EmojiItem("♨️", "hot springs", EmojiCategory.SYMBOLS, listOf("hot", "steam", "onsen")),
    EmojiItem("🛑", "stop sign", EmojiCategory.SYMBOLS, listOf("stop", "octagonal", "halt")),
    EmojiItem("🪧", "placard", EmojiCategory.SYMBOLS, listOf("sign", "protest", "board")),
    EmojiItem("🏧", "ATM sign", EmojiCategory.SYMBOLS, listOf("atm", "bank", "cash")),
    EmojiItem("🚮", "litter in bin sign", EmojiCategory.SYMBOLS, listOf("trash", "bin", "clean")),
    EmojiItem("🚰", "potable water", EmojiCategory.SYMBOLS, listOf("water", "tap", "drink")),
    EmojiItem("💢", "anger symbol", EmojiCategory.SYMBOLS, listOf("mad", "comic")),
    EmojiItem("💬", "speech balloon", EmojiCategory.SYMBOLS, listOf("chat", "message")),
    EmojiItem("💭", "thought balloon", EmojiCategory.SYMBOLS, listOf("think", "bubble")),
    EmojiItem("💤", "zzz", EmojiCategory.SYMBOLS, listOf("sleep", "tired")),
    EmojiItem("⚠️", "warning", EmojiCategory.SYMBOLS, listOf("alert", "caution")),
    EmojiItem("⛔", "no entry", EmojiCategory.SYMBOLS, listOf("stop", "forbidden")),
    EmojiItem("🚫", "prohibited", EmojiCategory.SYMBOLS, listOf("no", "ban")),
    EmojiItem("✅", "check mark button", EmojiCategory.SYMBOLS, listOf("yes", "correct", "done")),
    EmojiItem("✔️", "check mark", EmojiCategory.SYMBOLS, listOf("yes", "ok")),
    EmojiItem("❌", "cross mark", EmojiCategory.SYMBOLS, listOf("no", "wrong", "x")),
    EmojiItem("❎", "cross mark button", EmojiCategory.SYMBOLS, listOf("cancel", "x")),
    EmojiItem("❓", "question mark", EmojiCategory.SYMBOLS, listOf("what", "help")),
    EmojiItem("❗", "exclamation mark", EmojiCategory.SYMBOLS, listOf("attention", "important")),
    EmojiItem("🎵", "musical note", EmojiCategory.SYMBOLS, listOf("music", "song")),
    EmojiItem("🎶", "musical notes", EmojiCategory.SYMBOLS, listOf("music", "melody")),
    EmojiItem("💲", "heavy dollar sign", EmojiCategory.SYMBOLS, listOf("money", "dollar", "usd")),
    EmojiItem("™️", "trade mark", EmojiCategory.SYMBOLS, listOf("tm")),
    EmojiItem("©️", "copyright", EmojiCategory.SYMBOLS, listOf("c")),
    EmojiItem("®️", "registered", EmojiCategory.SYMBOLS, listOf("r")),
    EmojiItem("➕", "plus sign", EmojiCategory.SYMBOLS, listOf("add", "math")),
    EmojiItem("➖", "minus sign", EmojiCategory.SYMBOLS, listOf("subtract", "math")),
    EmojiItem("✖️", "multiply", EmojiCategory.SYMBOLS, listOf("times", "math")),
    EmojiItem("➗", "divide", EmojiCategory.SYMBOLS, listOf("division", "math")),
    EmojiItem("♾️", "infinity", EmojiCategory.SYMBOLS, listOf("forever", "infinite")),
    EmojiItem("🔝", "TOP arrow", EmojiCategory.SYMBOLS, listOf("top", "up")),
    EmojiItem("🔜", "SOON arrow", EmojiCategory.SYMBOLS, listOf("soon")),
    EmojiItem("🔛", "ON! arrow", EmojiCategory.SYMBOLS, listOf("on")),
    EmojiItem("🆗", "OK button", EmojiCategory.SYMBOLS, listOf("ok", "agree")),
    EmojiItem("🆕", "NEW button", EmojiCategory.SYMBOLS, listOf("new")),
    EmojiItem("🆓", "FREE button", EmojiCategory.SYMBOLS, listOf("free")),
    EmojiItem("🆙", "UP! button", EmojiCategory.SYMBOLS, listOf("up")),
    EmojiItem("🔴", "red circle", EmojiCategory.SYMBOLS, listOf("red", "round")),
    EmojiItem("🔵", "blue circle", EmojiCategory.SYMBOLS, listOf("blue", "round")),
    EmojiItem("🟢", "green circle", EmojiCategory.SYMBOLS, listOf("green", "round")),
    EmojiItem("🟡", "yellow circle", EmojiCategory.SYMBOLS, listOf("yellow", "round")),
    EmojiItem("⚫", "black circle", EmojiCategory.SYMBOLS, listOf("black", "round")),
    EmojiItem("⚪", "white circle", EmojiCategory.SYMBOLS, listOf("white", "round")),
    EmojiItem("⭐", "star", EmojiCategory.SYMBOLS, listOf("favorite", "star")),
    EmojiItem("🌟", "glowing star", EmojiCategory.SYMBOLS, listOf("shine", "glitter")),
    )
  }

  private val flags: List<EmojiItem> by lazy(LazyThreadSafetyMode.PUBLICATION) {
    listOf(
    EmojiItem("🇧🇩", "flag: Bangladesh", EmojiCategory.FLAGS, listOf("bangladesh", "bd", "bangla", "desh")),
    EmojiItem("🇮🇳", "flag: India", EmojiCategory.FLAGS, listOf("india", "in", "bharat")),
    EmojiItem("🇵🇰", "flag: Pakistan", EmojiCategory.FLAGS, listOf("pakistan", "pk")),
    EmojiItem("🇸🇦", "flag: Saudi Arabia", EmojiCategory.FLAGS, listOf("saudi", "arabia", "ksa")),
    EmojiItem("🇦🇪", "flag: United Arab Emirates", EmojiCategory.FLAGS, listOf("uae", "dubai")),
    EmojiItem("🇺🇸", "flag: United States", EmojiCategory.FLAGS, listOf("usa", "america", "us")),
    EmojiItem("🇬🇧", "flag: United Kingdom", EmojiCategory.FLAGS, listOf("uk", "britain", "england")),
    EmojiItem("🇨🇦", "flag: Canada", EmojiCategory.FLAGS, listOf("canada", "ca")),
    EmojiItem("🇦🇺", "flag: Australia", EmojiCategory.FLAGS, listOf("australia", "au")),
    EmojiItem("🇩🇪", "flag: Germany", EmojiCategory.FLAGS, listOf("germany", "de")),
    EmojiItem("🇫🇷", "flag: France", EmojiCategory.FLAGS, listOf("france", "fr")),
    EmojiItem("🇮🇹", "flag: Italy", EmojiCategory.FLAGS, listOf("italy", "it")),
    EmojiItem("🇪🇸", "flag: Spain", EmojiCategory.FLAGS, listOf("spain", "es")),
    EmojiItem("🇧🇷", "flag: Brazil", EmojiCategory.FLAGS, listOf("brazil", "br")),
    EmojiItem("🇦🇷", "flag: Argentina", EmojiCategory.FLAGS, listOf("argentina", "ar")),
    EmojiItem("🇯🇵", "flag: Japan", EmojiCategory.FLAGS, listOf("japan", "jp")),
    EmojiItem("🇰🇷", "flag: South Korea", EmojiCategory.FLAGS, listOf("korea", "kr")),
    EmojiItem("🇨🇳", "flag: China", EmojiCategory.FLAGS, listOf("china", "cn")),
    EmojiItem("🇷🇺", "flag: Russia", EmojiCategory.FLAGS, listOf("russia", "ru")),
    EmojiItem("🇹🇷", "flag: Turkey", EmojiCategory.FLAGS, listOf("turkey", "tr")),
    EmojiItem("🇲🇾", "flag: Malaysia", EmojiCategory.FLAGS, listOf("malaysia", "my")),
    EmojiItem("🇮🇩", "flag: Indonesia", EmojiCategory.FLAGS, listOf("indonesia", "id")),
    EmojiItem("🇪🇬", "flag: Egypt", EmojiCategory.FLAGS, listOf("egypt", "eg")),
    EmojiItem("🇿🇦", "flag: South Africa", EmojiCategory.FLAGS, listOf("south africa", "za")),
    EmojiItem("🇳🇬", "flag: Nigeria", EmojiCategory.FLAGS, listOf("nigeria", "ng")),
    EmojiItem("🇲🇽", "flag: Mexico", EmojiCategory.FLAGS, listOf("mexico", "mx")),
    EmojiItem("🇸🇬", "flag: Singapore", EmojiCategory.FLAGS, listOf("singapore", "sg")),
    EmojiItem("🇳🇱", "flag: Netherlands", EmojiCategory.FLAGS, listOf("netherlands", "holland")),
    EmojiItem("🇨🇭", "flag: Switzerland", EmojiCategory.FLAGS, listOf("switzerland", "swiss")),
    EmojiItem("🇸🇪", "flag: Sweden", EmojiCategory.FLAGS, listOf("sweden", "se")),
    EmojiItem("🇵🇹", "flag: Portugal", EmojiCategory.FLAGS, listOf("portugal", "pt")),
    EmojiItem("🇳🇴", "flag: Norway", EmojiCategory.FLAGS, listOf("norway", "no")),
    EmojiItem("🇩🇰", "flag: Denmark", EmojiCategory.FLAGS, listOf("denmark", "dk")),
    EmojiItem("🇫🇮", "flag: Finland", EmojiCategory.FLAGS, listOf("finland", "fi")),
    EmojiItem("🇳🇿", "flag: New Zealand", EmojiCategory.FLAGS, listOf("new zealand", "nz")),
    EmojiItem("🇮🇪", "flag: Ireland", EmojiCategory.FLAGS, listOf("ireland", "ie")),
    EmojiItem("🇳🇵", "flag: Nepal", EmojiCategory.FLAGS, listOf("nepal", "np")),
    EmojiItem("🇧🇹", "flag: Bhutan", EmojiCategory.FLAGS, listOf("bhutan", "bt")),
    EmojiItem("🇱🇰", "flag: Sri Lanka", EmojiCategory.FLAGS, listOf("sri lanka", "lk")),
    EmojiItem("🇲🇻", "flag: Maldives", EmojiCategory.FLAGS, listOf("maldives", "mv")),
    EmojiItem("🏁", "chequered flag", EmojiCategory.FLAGS, listOf("race", "finish")),
    EmojiItem("🚩", "triangular flag", EmojiCategory.FLAGS, listOf("red flag", "flag")),
    EmojiItem("🎌", "crossed flags", EmojiCategory.FLAGS, listOf("japan", "flags")),
    EmojiItem("🏴", "black flag", EmojiCategory.FLAGS, listOf("flag", "black")),
    EmojiItem("🏳️", "white flag", EmojiCategory.FLAGS, listOf("flag", "peace")),
    EmojiItem("🏳️‍🌈", "rainbow flag", EmojiCategory.FLAGS, listOf("pride", "lgbt", "rainbow")),
    EmojiItem("🏳️‍⚧️", "transgender flag", EmojiCategory.FLAGS, listOf("transgender", "pride")),
    EmojiItem("🏴‍☠️", "pirate flag", EmojiCategory.FLAGS, listOf("pirate", "skull")),
    )
  }

  val allEmojis: List<EmojiItem> by lazy(LazyThreadSafetyMode.PUBLICATION) {
    (smileys + people + animals + food + travel + activities + objects + symbols + flags)
      .distinctBy { it.unicode }
  }

  private val skinToneSupportedSet: Set<String> by lazy(LazyThreadSafetyMode.PUBLICATION) {
    val set = HashSet<String>(people.size * 2)
    for (item in people) {
      if (item.supportsSkinTone) {
        set.add(item.unicode)
        val clean = item.unicode.replace("\uFE0F", "")
        for (tone in EmojiSkinTone.entries) {
          if (tone != EmojiSkinTone.DEFAULT) {
            set.add("$clean${tone.modifier}")
          }
        }
      }
    }
    set
  }

  private val displayCategoryCache =
    java.util.concurrent.ConcurrentHashMap<Pair<EmojiCategory, EmojiSkinTone>, List<String>>()

  private val englishToBengaliSynonyms: Map<String, List<String>> = mapOf(
    "smile" to listOf("হাসি", "খুশি", "আনন্দ", "স্মাইল"),
    "happy" to listOf("খুশি", "আনন্দ", "হাসি", "সুখ"),
    "laugh" to listOf("হাসি", "মজা", "হাহা"),
    "joy" to listOf("আনন্দ", "খুশি", "হাসি"),
    "heart" to listOf("ভালোবাসা", "হৃদয়", "মন", "প্রেম", "দিল", "হার্ট"),
    "love" to listOf("ভালোবাসা", "প্রেম", "মন", "হৃদয়", "লাভ"),
    "kiss" to listOf("চুমু", "ভালোবাসা", "আদর"),
    "cry" to listOf("কান্না", "দুঃখ", "অশ্রু", "কষ্ট"),
    "sad" to listOf("দুঃখ", "মন খারাপ", "কষ্ট", "উদাস"),
    "angry" to listOf("রাগ", "ক্রোধ", "ক্ষোভ"),
    "fire" to listOf("আগুন", "গরম", "ফায়ার", "উত্তাপ"),
    "hot" to listOf("গরম", "আগুন", "তাপ"),
    "cold" to listOf("ঠান্ডা", "শীত", "বরফ"),
    "flag" to listOf("পতাকা", "ফ্ল্যাগ", "দেশ"),
    "bangladesh" to listOf("বাংলাদেশ", "বাংলা", "দেশ", "বিডি", "পতাকা"),
    "india" to listOf("ভারত", "ইন্ডিয়া"),
    "hand" to listOf("হাত", "আঙুল"),
    "wave" to listOf("হাত", "সালাম", "বিদায়", "হ্যালো"),
    "pray" to listOf("দোয়া", "প্রার্থনা", "নমস্কার", "সালাম"),
    "clap" to listOf("তালি", "হাততালি", "সাবাশ"),
    "thumbs" to listOf("লাইক", "ভালো", "হ্যাঁ", "ওকে"),
    "family" to listOf("পরিবার", "বাবা", "মা", "সন্তান"),
    "baby" to listOf("শিশু", "বাচ্চা", "খোকা"),
    "cat" to listOf("বিড়াল", "বেড়াল", "মিনি"),
    "dog" to listOf("কুকুর", "ছানা"),
    "tiger" to listOf("বাঘ", "টাইগার"),
    "lion" to listOf("সিংহ", "রাজা"),
    "bird" to listOf("পাখি", "টিয়া"),
    "fish" to listOf("মাছ", "ইলিশ"),
    "flower" to listOf("ফুল", "গোলাপ", "শাপলা"),
    "rose" to listOf("গোলাপ", "ফুল", "লাল"),
    "tree" to listOf("গাছ", "সবুজ", "প্রকৃতি"),
    "food" to listOf("খাবার", "খাদ্য", "রান্না"),
    "rice" to listOf("ভাত", "বিরিয়ানি", "খাবার"),
    "curry" to listOf("তরকারি", "ভাত", "খাবার"),
    "tea" to listOf("চা", "কাপ", "পানীয়"),
    "coffee" to listOf("কফি", "চা", "কাপ"),
    "cake" to listOf("কেক", "মিষ্টি", "জন্মদিন"),
    "mango" to listOf("আম", "ফল"),
    "apple" to listOf("আপেল", "ফল"),
    "banana" to listOf("কলা", "ফল"),
    "car" to listOf("গাড়ি", "কার", "যানবাহন"),
    "bus" to listOf("বাস", "গাড়ি"),
    "train" to listOf("ট্রেন", "রেলগাড়ি"),
    "plane" to listOf("বিমান", "উড়োজাহাজ", "ফ্লাইট"),
    "rocket" to listOf("রকেট", "মহাকাশ"),
    "home" to listOf("বাড়ি", "ঘর", "বাসা"),
    "mosque" to listOf("মসজিদ", "নামাজ", "ইসলাম"),
    "soccer" to listOf("ফুটবল", "খেলা", "বল"),
    "football" to listOf("ফুটবল", "খেলা", "বল"),
    "cricket" to listOf("ক্রিকেট", "খেলা", "ব্যাট"),
    "music" to listOf("গান", "সুর", "মিউজিক"),
    "trophy" to listOf("ট্রফি", "পুরস্কার", "জয়"),
    "phone" to listOf("ফোন", "মোবাইল", "কল"),
    "computer" to listOf("কম্পিউটার", "ল্যাপটপ"),
    "book" to listOf("বই", "পড়া", "শিক্ষা"),
    "money" to listOf("টাকা", "পয়সা", "ধন"),
    "gift" to listOf("উপহার", "গিফট"),
    "light" to listOf("আলো", "বাল্ব", "বাতি"),
    "star" to listOf("তারা", "নক্ষত্র", "স্টার"),
    "check" to listOf("টিক", "সঠিক", "হ্যাঁ"),
  )

  private val englishToHindiSynonyms: Map<String, List<String>> = mapOf(
    "smile" to listOf("मुस्कान", "हंसी", "खुशी", "स्माइल", "muskaan", "hasi", "khushi"),
    "happy" to listOf("खुशी", "खुश", "आनंद", "हंसी", "khushi", "khush"),
    "laugh" to listOf("हंसी", "मजाक", "हाहा", "hasi", "haha"),
    "joy" to listOf("आनंद", "खुशी", "हंसी", "anand", "khushi"),
    "heart" to listOf("दिल", "प्यार", "प्रेम", "हृदय", "हार्ट", "dil", "pyaar", "pyar"),
    "love" to listOf("प्यार", "प्रेम", "दिल", "मोहब्बत", "लव", "pyaar", "pyar", "dil"),
    "kiss" to listOf("चुम्मा", "प्यार", "किस", "chumma"),
    "cry" to listOf("रोना", "आंसू", "दुख", "rona", "aansu", "dukh"),
    "sad" to listOf("दुख", "उदास", "गम", "dukh", "udaas"),
    "angry" to listOf("गुस्सा", "क्रोध", "नाराज", "gussa"),
    "fire" to listOf("आग", "गर्म", "फायर", "aag", "garam"),
    "hot" to listOf("गर्म", "आग", "garam", "aag"),
    "cold" to listOf("ठंडा", "बर्फ", "सर्दी", "thanda", "barf"),
    "flag" to listOf("झंडा", "ध्वज", "देश", "jhanda", "desh"),
    "bangladesh" to listOf("बांग्लादेश", "बीडी", "bangladesh"),
    "india" to listOf("भारत", "इंडिया", "हिंदुस्तान", "bharat", "india"),
    "hand" to listOf("हाथ", "उंगली", "haath"),
    "wave" to listOf("हाथ", "नमस्ते", "सलाम", "हेलो", "haath", "namaste"),
    "pray" to listOf("प्रार्थना", "नमस्ते", "दुआ", "सलाम", "namaste", "prarthana", "dua"),
    "clap" to listOf("ताली", "शाबाश", "taali", "shabash"),
    "thumbs" to listOf("लाइक", "अच्छा", "हां", "ओके", "accha", "haan"),
    "family" to listOf("परिवार", "माता", "पिता", "बच्चे", "parivar"),
    "baby" to listOf("बच्चा", "शिशु", "baccha"),
    "cat" to listOf("बिल्ली", "billi"),
    "dog" to listOf("कुत्ता", "पिल्ला", "kutta"),
    "tiger" to listOf("बाघ", "टाइगर", "bagh"),
    "lion" to listOf("शेर", "राजा", "sher"),
    "bird" to listOf("पक्षी", "चिड़िया", "chidiya", "pakshi"),
    "fish" to listOf("मछली", "machli"),
    "flower" to listOf("फूल", "गुलाब", "phool", "gulab"),
    "rose" to listOf("गुलाब", "फूल", "लाल", "gulab", "phool"),
    "tree" to listOf("पेड़", "प्रकृति", "ped"),
    "food" to listOf("खाना", "भोजन", "khana", "bhojan"),
    "rice" to listOf("चावल", "बिरयानी", "खाना", "chawal", "biryani"),
    "curry" to listOf("सब्जी", "करी", "खाना", "sabzi"),
    "tea" to listOf("चाय", "कप", "chai"),
    "coffee" to listOf("कॉफी", "चाय", "कप", "coffee"),
    "cake" to listOf("केक", "मिठाई", "जन्मदिन", "mithai", "janamdin"),
    "mango" to listOf("आम", "फल", "aam", "phal"),
    "apple" to listOf("सेब", "फल", "seb"),
    "banana" to listOf("केला", "फल", "kela"),
    "car" to listOf("गाड़ी", "कार", "gaadi"),
    "bus" to listOf("बस", "गाड़ी", "gaadi"),
    "train" to listOf("ट्रेन", "रेलगाड़ी", "railgadi"),
    "plane" to listOf("विमान", "हवाई जहाज", "फ्लाइट", "viman"),
    "rocket" to listOf("रॉकेट", "अंतरिक्ष", "antariksh"),
    "home" to listOf("घर", "मकान", "ghar"),
    "mosque" to listOf("मस्जिद", "नमाज", "masjid", "namaz"),
    "soccer" to listOf("फुटबॉल", "खेल", "गेंद", "khel"),
    "football" to listOf("फुटबॉल", "खेल", "गेंद", "khel"),
    "cricket" to listOf("क्रिकेट", "खेल", "बल्ला", "khel"),
    "music" to listOf("संगीत", "गाना", "म्यूजिक", "gaana", "sangeet"),
    "trophy" to listOf("ट्रॉफी", "इनाम", "जीत", "jeet", "inaam"),
    "phone" to listOf("फोन", "मोबाइल", "कॉल", "mobile"),
    "computer" to listOf("कंप्यूटर", "लैपटॉप", "computer"),
    "book" to listOf("किताब", "पढ़ाई", "शिक्षा", "kitab", "padhai"),
    "money" to listOf("पैसा", "रुपया", "धन", "paisa", "rupaya"),
    "gift" to listOf("उपहार", "गिफ्ट", "तोहफा", "tohfa", "uphaar"),
    "light" to listOf("रोशनी", "बल्ब", "बत्ती", "roshni", "batti"),
    "star" to listOf("तारा", "सितारा", "स्टार", "tara", "sitara"),
    "check" to listOf("सही", "टिक", "हां", "sahi", "haan"),
  )

  private class IndexedEmojiEntry(
    val item: EmojiItem,
    val unicode: String,
    val searchBlob: String,
  )

  private val indexedEntries: List<IndexedEmojiEntry> by lazy(LazyThreadSafetyMode.PUBLICATION) {
    allEmojis.map { item ->
      val terms = LinkedHashSet<String>(16)
      val nameLower = item.name.lowercase()
      terms.add(nameLower)
      nameLower.split(" ", "-", ":", "(", ")", ",").filter { it.isNotEmpty() }.forEach { word ->
        terms.add(word)
        englishToBengaliSynonyms[word]?.forEach { bnSyn ->
          terms.add(bnSyn)
          terms.add(com.example.voice.BanglishTransliterator.transliterate(bnSyn).lowercase())
        }
        englishToHindiSynonyms[word]?.forEach { hiSyn ->
          terms.add(hiSyn.lowercase())
        }
      }
      item.keywords.forEach { kw ->
        val kwLower = kw.lowercase()
        terms.add(kwLower)
        englishToBengaliSynonyms[kwLower]?.forEach { bnSyn ->
          terms.add(bnSyn)
          terms.add(com.example.voice.BanglishTransliterator.transliterate(bnSyn).lowercase())
        }
        englishToHindiSynonyms[kwLower]?.forEach { hiSyn ->
          terms.add(hiSyn.lowercase())
        }
      }
      val catLower = item.category.displayName.lowercase()
      terms.add(catLower)
      IndexedEmojiEntry(
        item = item,
        unicode = item.unicode,
        searchBlob = terms.joinToString("\u0000"),
      )
    }
  }

  private const val MAX_SEARCH_CACHE_ENTRIES = 64
  private val searchItemCache =
    java.util.concurrent.ConcurrentHashMap<String, List<EmojiItem>>()
  private val searchUnicodeCache =
    java.util.concurrent.ConcurrentHashMap<String, List<String>>()

  fun getEmojisForCategory(category: EmojiCategory): List<EmojiItem> {
    return when (category) {
      EmojiCategory.RECENT -> emptyList()
      EmojiCategory.SMILEYS -> smileys
      EmojiCategory.PEOPLE -> people
      EmojiCategory.ANIMALS -> animals
      EmojiCategory.FOOD -> food
      EmojiCategory.TRAVEL -> travel
      EmojiCategory.ACTIVITIES -> activities
      EmojiCategory.OBJECTS -> objects
      EmojiCategory.SYMBOLS -> symbols
      EmojiCategory.FLAGS -> flags
    }
  }

  fun supportsSkinTone(unicode: String): Boolean {
    return unicode in skinToneSupportedSet
  }

  fun getDisplayEmojisForCategory(
    category: EmojiCategory,
    skinTone: EmojiSkinTone = EmojiSkinTone.DEFAULT,
  ): List<String> {
    if (category == EmojiCategory.RECENT) return emptyList()
    val effectiveTone = if (category == EmojiCategory.PEOPLE) skinTone else EmojiSkinTone.DEFAULT
    val key = category to effectiveTone
    return displayCategoryCache.getOrPut(key) {
      val items = getEmojisForCategory(category)
      val result = ArrayList<String>(items.size)
      val seen = HashSet<String>(items.size)
      for (item in items) {
        val formatted =
          if (item.supportsSkinTone && effectiveTone != EmojiSkinTone.DEFAULT) {
            val clean = item.unicode.replace("\uFE0F", "")
            "$clean${effectiveTone.modifier}"
          } else {
            item.unicode
          }
        if (seen.add(formatted)) {
          result.add(formatted)
        }
      }
      result
    }
  }

  /**
   * Search through all emojis by English name, keywords, Bengali (Probhat) / Banglish keywords,
   * and Hindi (Devanagari / Hinglish) keywords.
   * Case-insensitive and guaranteed distinct by unicode.
   */
  fun search(query: String, rawAsciiQuery: String = ""): List<EmojiItem> {
    val clean = query.trim().lowercase().replace("\u0000", "")
    val rawCleanCandidate = rawAsciiQuery.trim().lowercase().replace("\u0000", "")
    val cleanRaw = if (rawCleanCandidate == clean) "" else rawCleanCandidate
    if (clean.isEmpty() && cleanRaw.isEmpty()) return emptyList()

    val cacheKey = if (cleanRaw.isEmpty()) clean else "$clean\u0001$cleanRaw"
    searchItemCache[cacheKey]?.let { return it }

    val entries = indexedEntries
    val matchedItems = ArrayList<EmojiItem>(24)
    val matchedUnicodes = ArrayList<String>(24)
    for (i in entries.indices) {
      val entry = entries[i]
      val matches =
        (clean.isNotEmpty() && (entry.unicode.contains(clean) || entry.searchBlob.contains(clean))) ||
          (cleanRaw.isNotEmpty() && entry.searchBlob.contains(cleanRaw))
      if (matches) {
        matchedItems.add(entry.item)
        matchedUnicodes.add(entry.unicode)
      }
    }

    if (searchItemCache.size >= MAX_SEARCH_CACHE_ENTRIES) {
      searchItemCache.clear()
      searchUnicodeCache.clear()
    }
    searchItemCache[cacheKey] = matchedItems
    searchUnicodeCache[cacheKey] = matchedUnicodes
    return matchedItems
  }

  /**
   * Returns cached distinct unicode strings matching [query] and [rawAsciiQuery] without allocating
   * intermediate lists on every Compose recomposition.
   */
  fun searchUnicode(query: String, rawAsciiQuery: String = ""): List<String> {
    val clean = query.trim().lowercase().replace("\u0000", "")
    val rawCleanCandidate = rawAsciiQuery.trim().lowercase().replace("\u0000", "")
    val cleanRaw = if (rawCleanCandidate == clean) "" else rawCleanCandidate
    if (clean.isEmpty() && cleanRaw.isEmpty()) return emptyList()

    val cacheKey = if (cleanRaw.isEmpty()) clean else "$clean\u0001$cleanRaw"
    searchUnicodeCache[cacheKey]?.let { return it }
    search(query, rawAsciiQuery)
    return searchUnicodeCache[cacheKey] ?: emptyList()
  }
}
