/*
 * Original work by [SkyblockAPI](https://github.com/SkyblockAPI/SkyblockAPI) and contributors (MIT License).
 * The MIT License (MIT)
 *
 * Copyright (c) 2025
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * Modifications:
 *   Copyright (c) 2025 skies-starred
 *   Licensed under the BSD 3-Clause License.
 *
 * The original MIT license applies to the portions derived from SkyblockAPI.
 */
package xyz.aerii.athen.api.location

import xyz.aerii.athen.handlers.React

enum class SkyBlockArea(val key: String, val displayName: String) {
    NONE("none", "None"),
    PRIVATE_ISLAND("private_island", "Your Island"),
    GARDEN("garden", "The Garden"),

    // Hub
    VILLAGE("village", "Village"),
    FOREST("forest", "Forest"),
    PET_CARE("pet_care", "Pet Care"),
    FARM("farm", "Farm"),
    ARTISTS_ABODE("artists_abode", "Artist's Abode"),
    COLOSSEUM("colosseum", "Colosseum"),
    FASHION_SHOP("fashion_shop", "Fashion Shop"),
    FLOWER_HOUSE("flower_house", "Flower House"),
    CANVAS_ROOM("canvas_room", "Canvas Room"),
    MOUNTAIN("mountain", "Mountain"),
    BANK("bank", "Bank"),
    AUCTION_HOUSE("auction_house", "Auction House"),
    SHENS_AUCTION("shens_auction", "Shen's Auction"),
    COMMUNITY_CENTER("community_center", "Community Center"),
    ELECTION_ROOM("election_room", "Election Room"),
    FARMHOUSE("farmhouse", "Farmhouse"),
    WEAPONSMITH("weaponsmith", "Weaponsmith"),
    BLACKSMITH("blacksmith", "Blacksmith"),
    ARCHERY_RANGE("archery_range", "Archery Range"),
    LIBRARY("library", "Library"),
    HEXATORUM("hexatorium", "Hexatorium"),
    TRADE_CENTER("trade_center", "Trade Center"),
    BUILDERS_HOUSE("builders_house", "Builder's House"),
    TAVERN("tavern", "Tavern"),
    GRAVEYARD("graveyard", "Graveyard"),
    COAL_MINE("coal_mine", "Coal Mine"),
    BAZAAR_ALLEY("bazaar_alley", "Bazaar Alley"),
    WILDERNESS("wilderness", "Wilderness"),
    FISHING_OUTPOST("fishing_outpost", "Fishing Outpost"),
    FISHERMANS_HUT("fishermans_hut", "Fisherman's Hut"),
    UNINCORPORATED("unincorporated", "Unincorporated"),
    WIZARD_TOWER("wizard_tower", "Wizard Tower"),
    RUINS("ruins", "Ruins"),

    // Rift
    WYLD_WOODS("wyld_woods", "Wyld Woods"),
    THE_BASTION("the_bastion", "The Bastion"),
    BROKEN_CAGE("broken_cage", "Broken Cage"),
    SHIFTED_TAVERN("shifted_tavern", "Shifted Tavern"),
    BLACK_LAGOON("black_lagoon", "Black Lagoon"),
    LAGOON_CAVE("lagoon_cave", "Lagoon Cave"),
    OTHERSIDE("otherside", "Otherside"),
    LEECHES_LAIR("leeches_lair", "Leeches Lair"),
    LAGOON_HUT("lagoon_hut", "Lagoon Hut"),
    AROUND_COLOSSEUM("around_colosseum", "Around Colosseum"),
    WEST_VILLAGE("west_village", "West Village"),
    DOPLHIN_TRAINER("dolphin_trainer", "Dolphin Trainer"),
    INFESTED_HOUSE("infested_house", "Infested House"),
    DREADFARM("dreadfarm", "Dreadfarm"),
    MIRRORVERSE("mirrorverse", "Mirrorverse"),
    CAKE_HOUSE("cake_house", "Cake House"),
    VILLAGE_PLAZA("village_plaza", "Village Plaza"),
    MURDER_HOUSE("murder_house", "Murder House"),
    TAYLORS("taylors", "Taylor's"),
    HALF_EATEN_CAVE("half_eaten_cave", "Half-Eaten Cave"),
    BOOK_IN_A_BOOK("book_in_a_book", "Book in a Book"),
    EMPTY_BANK("empty_bank", "Empty Bank"),
    BARRIER_STREET("barrier_street", "Barrier Street"),
    BARRY_CENTER("barry_center", "Barry Center"),
    BARRY_HQ("barry_hq", "Barry HQ"),
    RIFT_GALLERY("rift_gallery", "Rift Gallery"),
    RIFT_GALLERY_ENTRANCE("rift_gallery_entrance", "Rift Gallery Entrance"),
    THE_MOUNTAINTOP("the_mountaintop", "The Mountaintop"),
    WIZARDMAN_BUREAU("wizardman_bureau", "Wizardman Bureau"),
    THE_VENTS("the_vents", "The Vents"),
    CEREBRAL_CITADEL("cerebral_citadel", "Cerebral Citadel"),
    WALK_OF_FAME("walk_of_fame", "Walk of Fame"),
    TRIAL_GROUNDS("trial_grounds", "Trial Grounds"),
    CONTINUUM("continuum", "Continuum"),
    TIME_CHAMBER("time_chamber", "Time Chamber"),

    // Rift-Slayer
    PHOTON_PATHWAY("photon_pathway", "Photon Pathway"),
    STILLGORE_CHATEAU("stillgore_chateau", "Stillgore Château"),
    OUBLIETTE("oubliette", "Oubliette"),
    FAIRYLOSOPHER_TOWER("fairylosopher_tower", "Fairylosopher Tower"),

    // Dwarves
    BASECAMP("basecamp", "Dwarven Base Camp"),
    FOSSIL_RESEARCH("fossil_research", "Fossil Research Center"),
    GLACITE_TUNNELS("glacite_tunnels", "Glacite Tunnels"),
    GREAT_LAKE("great_lake", "Great Glacite Lake"),

    // Crimson
    DOJO("dojo", "Dojo"),
    DOJO_ARENA("dojo_arena", "Dojo Arena"),
    MAGMA_CHAMBER("magma_chamber", "Magma Chamber"),
    CRIMSON_ISLE("crimson_isle", "Crimson Isle"),
    CRIMSON_FIELDS("crimson_fields", "Crimson Fields"),
    BURNING_DESERT("burning_desert", "Burning Desert"),
    DRAGONTAIL("dragontail", "Dragontail"),
    DRAGONTAIL_BLACKSMITH("dragontail_blacksmith", "Dragontail Blacksmith"),
    DRAGONTAIL_BANK("dragontail_bank", "Dragontail Bank"),
    DRAGONTAIL_TOWNSQUARE("dragontail_townsquare", "Dragontail Townsquare"),
    DRAGONTAIL_AUCTION_HOUS("dragontail_auction_hous", "Dragontail Auction Hous"),
    MINION_SHOP("minion_shop", "Minion Shop"),
    THE_DUKEDOM("the_dukedom", "The Dukedom"),
    BLAZING_VOLCANO("blazing_volcano", "Blazing Volcano"),
    ODGER_HUT("odger_hut", "Odger's Hut"),
    THE_WASTELAND("the_wasteland", "The Wasteland"),
    FORGOTTEN_SKULL("forgotten_skull", "Forgotten Skull"),
    SCARLETON("scarleton", "Scarleton"),
    COURTYARD("courtyard", "Courtyard"),
    SCARLETON_BANK("scarleton_bank", "Scarleton Bank"),
    SCARLETON_PLAZA("scarleton_plaza", "Scarleton Plaza"),
    SCARLETON_AUCTION_HOUSE("scarleton_auction_house", "Scarleton Auction House"),
    SCARLETON_BAZAAR("scarleton_bazaar", "Scarleton Bazaar"),
    SCARLETON_MINION_SHOP("scarleton_minion_shop", "Scarleton Minion Shop"),
    SCARLETON_BLACKSMITH("scarleton_blacksmith", "Scarleton Blacksmith"),
    CATHEDRAL("cathedral", "Cathedral"),
    MYSTIC_MARSH("mystic_marsh", "Mystic Marsh"),
    MATRIARCH_LAIR("matriarch_lair", "Matriarch's Lair"),
    BELLY_OF_THE_BEAST("belly_of_the_beast", "Belly of the Beast"),
    SMOLDERING_TOMB("smoldering_tomb", "Smoldering Tomb"),

    // Jerry
    GLACIAL_CAVE("glacial_cave", "Glacial Cave"),
    MOUNT_JERRY("mount_jerry", "Mount Jerry"),
    HOT_SPRINGS("hot_springs", "Hot Springs"),
    JERRY_POND("jerry_pond", "Jerry Pond"),
    REFLECTIVE_POND("reflective_pond", "Reflective Pond"),
    TERRYS_SHACK("terrys_shack", "Terry's Shack"),
    SUNKEN_JERRY_POND("sunken_jerry_pond", "Sunken Jerry Pond"),
    EINARYS_EMPORIUM("einarys_emporioum", "Einary's Emporium"),
    SHERRYS_SHOWROOM("sherrys_showroom", "Sherry's Showroom"),
    GARYS_SHACK("garys_shack", "Gary's Shack"),

    // Spider
    SPIDER_MOUND("spider_mound", "Spider Mound"),
    GRAVEL_MINES("gravel_mines", "Gravel Mines"),
    GRANDMAS_HOUSE("grandmas_house", "Grandma's House"),
    ARACHNES_BURROW("arachnes_burrow", "Arachne's Burrow"),
    ARACHNES_SANCTUARY("arachnes_sanctuary", "Arachne's Sanctuary"),
    ARCHAEOLOGISTS_CAMP("archaeologists_camp", "Archaeologist's Camp"),

    // End
    DRAGONS_NEST("dragons_nest", "Dragon's Nest"),
    VOID_SEPULTURE("void_sepulture", "Void Sepulture"),
    VOID_SLATE("void_slate", "Void Slate"),
    ZEALOT_BRUISER_HIDEOUT("zealot_bruiser_hideout", "Zealot Bruiser Hideout"),

    // Farming Islands
    THE_BARN("the_barn", "The Barn"),
    MUSHROOM_DESERT("mushroom_desert", "Mushroom Desert"),
    WINDMILL("windmill", "Windmill"),
    DESERT_SETTLEMENT("desert_settlement", "Desert Settlement"),
    GLOWING_MUSHROOM_CAVE("glowing_mushroom_cave", "Glowing Mushroom Cave"),
    JAKES_HOUSE("jakes_house", "Jake's House"),
    MUSHROOM_GORGE("mushroom_gorge", "Mushroom Gorge"),
    OASIS("oasis", "Oasis"),
    OVERGROWN_MUSHROOM_CAVE("overgrown_mushroom_cave", "Overgrown Mushroom Cave"),
    SHEPHERDS_KEEP("shepherds_keep", "Shepherd's Keep"),
    TRAPPERS_DEN("trappers_den", "Trapper's Den"),
    TREASURE_HUNTER_CAMP("treasure_hunter_camp", "Treasure Hunter Camp"),

    // Park
    BIRCH_PARK("birch_park", "Birch Park"),
    HOWLING_CAVE("howling_cave", "Howling Cave"),
    SOUL_CAVE("soul_cave", "Soul Cave"),
    SPIRIT_CAVE("spirit_cave", "Spirit Cave"),
    SPRUCE_WOODS("spruce_woods", "Spruce Woods"),
    LONELY_ISLAND("lonely_island", "Lonely Island"),
    VIKING_LONGHOUSE("viking_longhouse", "Viking Longhouse"),
    DARK_THICKET("dark_thicket", "Dark Thicket"),
    SAVANNA_WOODLAND("savanna_woodland", "Savanna Woodland"),
    MELODYS_PLATEAU("melodys_plateau", "Melody's Plateau"),
    JUNGLE_ISLAND("jungle_island", "Jungle Island"),

    // Deep Caverns
    DEEP_CAVERNS("deep_caverns", "Deep Caverns"),
    GUNPOWDER_MINES("gunpowder_mines", "Gunpowder Mines"),
    LAPIS_QUARRY("lapis_quarry", "Lapis Quarry"),
    PIGMENS_DEN("pigmens_den", "Pigmen's Den"),
    SLIMEHILL("slimehill", "Slimehill"),
    DIAMOND_RESERVE("diamond_reserve", "Diamond Reserve"),
    OBSIDIAN_SANCTUARY("obsidian_sanctuary", "Obsidian Sanctuary"),

    // Crystal Hollows
    CRYSTAL_NUCLEUS("crystal_nucleus", "Crystal Nucleus"),
    GOBLIN_HOLDOUT("goblin_holdout", "Goblin Holdout"),
    GOBLIN_QUEENS_DEN("goblin_queens_den", "Goblin Queen's Den"),
    JUNGLE("jungle", "Jungle"),
    JUNGLE_TEMPLE("jungle_temple", "Jungle Temple"),
    PRECURSOR_REMNANTS("precursor_remnants", "Precursor Remnants"),
    LOST_PRECURSOR_CITY("lost_precursor_city", "Lost Precursor City"),
    MITHRIL_DEPOSITS("mithril_deposits", "Mithril Deposits"),
    DRAGONS_LAIR("dragons_lair", "Dragon's Lair"),
    MINES_OF_DIVAN("mines_of_divan", "Mines of Divan"),
    MAGMA_FIELDS("magma_fields", "Magma Fields"),
    KHAZAD_DUM("khazad_dum", "Khazad-dûm"),
    FAIRY_GROTTO("fairy_grotto", "Fairy Grotto"),

    // Backwater Bayou
    BACKWATER_BAYOU("backwater_bayou", "Backwater Bayou"),

    // Galatea
    TANGLEBURG_PATH("tangleburg_path", "Tangleburg's Path"),
    TANGLEBURG("tangleburg", "Tangleburg"),
    NORTH_REACHES("north_reaches", "North Reaches"),
    WEST_REACHES("west_reaches", "West Reaches"),
    SOUTH_REACHES("south_reaches", "South Reaches"),
    MOONGLADE_MARSH("moonglade_marsh", "Moonglade Marsh"),
    MOONGLADE_EDGE("moonglade_edge", "Moonglade's Edge"),
    VERDANT_SUMMIT("verdant_summit", "Verdant Summit"),
    NORTH_WETLANDS("north_wetlands", "North Wetlands"),
    WESTBOUND_WETLANDS("westbound_wetlands", "Westbound Wetlands"),
    SOUTH_WETLANDS("south_wetlands", "South Wetlands"),
    MURKWATER_LOCH("murkwater_loch", "Murkwater Loch"),
    WYRMGROVE_TOMB("wyrmgrove_tomb", "Wyrmgrove Tomb"),
    EVERGREEN_PLATEAU("evergreen_plateau", "Evergreen Plateau"),
    MURKWATER_OUTPOST("murkwater_outpost", "Murkwater Outpost"),
    MURKWATER_DEPTHS("murkwater_depths", "Murkwater Depths"),
    ANCIENT_RUINS("ancient_ruins", "Ancient Ruins"),
    MURKWATER_SHALLOWS("murkwater_shallows", "Murkwater Shallows"),
    DIVE_EMBER_PASS("dive_ember_pass", "Dive-Ember Pass"),
    STRIDE_EMBER_FISSURE("stride_ember_fissure", "Stride-Ember Fissure"),
    SIDE_EMBER_WAY("side_ember_way", "Side-Ember Way"),
    REEFGUARD_DEPTHS("reefguard_depths", "Reefguard Depths"),
    REEFGUARD_PASS("reefguard_pass", "Reefguard Pass"),
    DROWNED_RELIQUARY("drowned_reliquary", "Drowned Reliquary"),
    BUBBLEBOOST_COLUMN("bubbleboost_column", "Bubbleboost Column"),
    KELPWOVEN_TUNNELS("kelpwoven_tunnels", "Kelpwoven Tunnels"),
    RED_HOUSE("red_house", "Red House"),
    TOMB_FLOODWAY("tomb_floodway", "Tomb Floodway"),
    DRIPTOAD_DELVE("driptoad_delve", "Driptoad Delve"),
    DRIPTOAD_PASS("driptoad_pass", "Driptoad Pass"),
    TANGLEBURG_BANK("tangleburg_bank", "Tangleburg Bank"),
    FUSION_HOUSE("fusion_house", "Fusion House"),
    SWAMP_CUT_INC("swamp_cut_inc", "SwampCut Inc."),
    TANGLEBURG_LIBRARY("tangleburg_library", "Tangleburg Library"),
    FOREST_TEMPLE("forest_temple", "Forest Temple"),
    TRANQUILITY_SANCTUM("tranquility_sanctum", "Tranquility Sanctum"),
    TRANQUIL_PASS("tranquil_pass", "Tranquil Pass"),

    // Dwarven Mines
    THE_LIFT("the_lift", "The Lift"),
    DWARVEN_VILLAGE("dwarven_village", "Dwarven Village"),
    DWARVEN_MINES("dwarven_mines", "Dwarven Mines"),
    LAVA_SPRINGS("lava_springs", "Lava Springs"),
    PALACE_BRIDGE("palace_bridge", "Palace Bridge"),
    ROYAL_PALACE("royal_palace", "Royal Palace"),
    GRAND_LIBRARY("grand_library", "Grand Library"),
    ROYAL_QUARTERS("royal_quarters", "Royal Quarters"),
    BARRACKS_OF_HEROES("barracks_of_heroes", "Barracks of Heroes"),
    HANGING_COURT("hanging_court", "Hanging Court"),
    GREAT_ICE_WALL("great_ice_wall", "Great Ice Wall"),
    ARISTOCRAT_PASSAGE("aristocrat_passage", "Aristocrat Passage"),
    ROYAL_MINES("royal_mines", "Royal Mines"),
    THE_MIST("the_mist", "The Mist"),
    DIVANS_GATEWAY("divans_gateway", "Divan's Gateway"),
    CLIFFSIDE_VEINS("cliffside_veins", "Cliffside Veins"),
    FORGE_BASIN("forge_basin", "Forge Basin"),
    THE_FORGE("the_forge", "The Forge"),
    RAMPARTS_QUARRY("ramparts_quarry", "Rampart's Quarry"),
    FAR_RESERVE("far_reserve", "Far Reserve"),
    UPPER_MINES("upper_mines", "Upper Mines"),
    ABANDONED_QUARRY("abandoned_quarry", "Abandoned Quarry")
    ;

    val inArea: React<Boolean>
        get() = LocationAPI.area.map { it == this }

    override fun toString() = displayName

    companion object {
        fun getByKey(key: String) = entries.firstOrNull { it.displayName == key }

        fun inAnyArea(vararg areas: SkyBlockArea) = LocationAPI.area.map { it in areas }
    }
}
