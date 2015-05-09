import java.util.Random;

public final class KHANG {
    private static Random randomness;

    private static final String[] adjectives = {
        "Large", "Big", "Tiny", "Little", "Small", "Odious", "Orange", "Blue", "Purple", "Green",
        "Yellow", "Beige", "Teal", "Magenta", "Fast", "Slow", "Quick", "Burning", "Sad", "Grape",
        "Central", "Offset", "Epoxy", "Dry", "Moist", "Damp", "Soggy", "Bold", "Timid", "Anti-",
        "Partial", "Long", "Happy", "Hot", "Pretty", "Awful", "Silky", "Smooth", "Salty", "Sulky",
        "Smart", "Joyful", "Gallant", "Rude", "Beautiful", "Billious", "Bountiful", "Bodacious",
        "Beautiful", "Cautious", "Clingy", "Colourful", "Bright", "Dark", "Dull", "Matte",
        "Demonic", "Angelic", "Boxy", "Dutiful", "Dowdy", "Downy", "Elastic", "Stretchy", "Smelly",
        "Fragrant", "Enduring", "Fancy", "Boring", "Ugly", "Foul", "Foolish", "Gigantic", "Gawky",
        "Spheroid", "Flatulent", "Heroic", "Clever", "Inky", "Ignoble", "Noble", "Jolly",
        "Jacobian", "Jealous", "Jumpy", "Juicy", "Kindly", "Caustic", "Limited", "Corrosive",
        "Limitless", "Flying", "Biased", "Silly", "Mangy", "Nutty", "Crazy", "New", "Old", "Young",
        "Elderly", "Teenage", "Zany", "Bendy", "Rigid", "Ductile", "Horrifying", "Horrid",
        "Slippery", "Shiny", "Repulsive", "Tasty", "Mr", "Mrs", "Sir", "Lord", "Admiral", "Captain",
        "Commander", "Commadore", "Leading", "Feline", "Canine", "Electric", "Magnetic",
        "Disturbing", "Unsettling", "Distressing","Stressful", "Stressed","Depressed","Gyratring",
        "Doozy",  "16.2-Ounce ", "Exploding", "Octagonal", "Amusing", "Integrable", "Dancing",
        "Squishy", "Painted", "Runny", "Flamboyant", "Retriculating", "Insectoid", "Chitinous",
        "Wobbly"
    };
    private static final String[] nouns = {
        "Potato", "Chair","Farmer", "Table", "Pencil", "Pen", "Ruler", "Hat", "Dog", "Moose", "Cat",
        "Mouse", "Dancer", "Formula", "Sombrero", "Plant", "Star", "Curtain", "Towel", "Stool",
        "Box", "Notebook", "Disc", "Sphere", "Phone", "Biscuit", "Cake", "Meatloaf", "Bread",
        "Starfish", "Shark", "Laser", "Turnip", "Pear", "Apple", "Bridge", "House", "Screen",
        "Mint", "Celery", "Parsely", "Olive", "Tomato", "Cylinder", "Potato", "Pea", "Tea", "Poppy",
        "Bean", "Rose", "Daisy", "Bottle", "Container", "Street", "Drawer", "Trousers", "Shirt",
        "Cap", "Mouthwash", "Jacket", "Gown", "Coat", "Wallet", "Card", "Article", "Umbrella",
        "Soap", "Brush", "Broom", "Basin", "Zebra", "Cat", "Kitten", "Cow", "Donkey", "Horse",
        "Penguin", "Clock", "Snail", "Mouse", "Grass", "Lawn", "Squirrel", "Concrete", "Tarmac",
        "Lamp", "Light", "Hedge", "Tree", "Roof", "Road", "Path", "Meter", "Center", "Carpet",
        "Sock", "Bowtie", "Tie", "Scarf", "Rope", "String", "Pill", "Tablet", "Wire", "Surgeon",
        "Fire", "Postman", "Hurdle", "Sanitiser", "Discus", "Bucket", "Barrow", "Can", "Colander",
        "Tuna", "Herring", "Vitamin", "File", "Hinge", "Flannel", "Fan", "Perfume", "Tin", "Bolt",
        "Hook", "Arm", "Leg", "Handle", "Hair", "Cosy", "Teapot", "Teacup", "Teatime", "Straw",
        "Beer", "Mead", "Salmon", "Spirit", "Warship", "Boat", "Whale", "Oyster", "Meercat",
        "Haircut", "Toaster", "Kettle", "Revolver", "Moon", "Rocket", "Wheel", "Rhubarb", "Spline",
        "Popcorn", "Furphy", "Fartlek", "Cake Mix", "Egg", "Tuna", "Starfish", "Seahorse",
        "Firework", "Camel", "Elvis", "Carp"
    };

    public static String getAdjective() {
        int adj = (int) Math.round(randomness.nextDouble() * (adjectives.length - 1));
        return adjectives[adj];
    }

    public static String getNoun() {
        int nou = (int) Math.round(randomness.nextDouble() * (nouns.length - 1));
        return nouns[nou];
    }

    public static String getName() {
        return getAdjective() + getNoun();
    }

    // Because clearly, it's vitally important we can get deterministic silly names.
    public static void seed(long seed) {
        randomness = new Random(seed);
    }
}
