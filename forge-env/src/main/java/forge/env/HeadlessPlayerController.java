package forge.env;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import forge.LobbyPlayer;
import forge.card.ColorSet;
import forge.card.ICardFace;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostShard;
import forge.deck.Deck;
import forge.game.*;
import forge.game.ability.effects.RollDiceEffect;
import forge.game.card.*;
import forge.game.combat.Combat;
import forge.game.cost.Cost;
import forge.game.cost.CostPart;
import forge.game.cost.CostPartMana;
import forge.game.keyword.KeywordInterface;
import forge.game.mana.Mana;
import forge.game.mana.ManaConversionMatrix;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.player.*;
import forge.game.replacement.ReplacementEffect;
import forge.game.spellability.*;
import forge.game.staticability.StaticAbility;
import forge.game.trigger.WrappedAbility;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.util.ITriggerEvent;
import forge.util.MyRandom;
import forge.util.collect.FCollectionView;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.function.Predicate;

/**
 * Headless player controller that makes minimal decisions to support game initialization.
 * For V1, automatically keeps opening hands and doesn't take any game actions.
 */
class HeadlessPlayerController extends PlayerController {

    public HeadlessPlayerController(Game game, Player player, LobbyPlayer lobbyPlayer) {
        super(game, player, lobbyPlayer);
    }

    @Override
    public boolean isAI() {
        return false;
    }

    // Mulligan handling - always keep
    @Override
    public boolean mulliganKeepHand(Player player, int cardsToReturn) {
        return true; // Always keep opening hand
    }

    @Override
    public CardCollectionView orderMoveToZoneList(CardCollectionView cards, ZoneType destinationZone, SpellAbility source) {
        return cards;
    }

    @Override
    public List<PaperCard> sideboard(Deck deck, GameType gameType, String message) {
        return null; // No sideboarding
    }

    @Override
    public Map<Card, Integer> assignCombatDamage(Card attacker, CardCollectionView blockers, CardCollectionView remaining, int damageDealt, GameEntity defender, boolean overrideOrder) {
        // Default damage assignment
        Map<Card, Integer> result = new HashMap<>();
        if (!blockers.isEmpty()) {
            result.put(blockers.getFirst(), damageDealt);
        }
        return result;
    }

    @Override
    public Integer announceRequirements(SpellAbility ability, String announce) {
        return 0;
    }

    @Override
    public CardCollectionView choosePermanentsToSacrifice(SpellAbility sa, int min, int max, CardCollectionView validTargets, String message) {
        return new CardCollection();
    }

    @Override
    public CardCollectionView choosePermanentsToDestroy(SpellAbility sa, int min, int max, CardCollectionView validTargets, String message) {
        return new CardCollection();
    }

    @Override
    public TargetChoices chooseNewTargetsFor(SpellAbility ability, Predicate<GameObject> filter, boolean optional) {
        return null;
    }

    @Override
    public boolean chooseBinary(SpellAbility sa, String question, BinaryChoiceType kindOfChoice, Boolean defaultVal) {
        if (kindOfChoice == BinaryChoiceType.PlayOrDraw) {
            // Randomly choose play or draw
            return MyRandom.getRandom().nextBoolean();
        }
        return defaultVal != null ? defaultVal : false;
    }

    @Override
    public boolean chooseDirection(SpellAbility sa, String prompt, List<Direction> possibleDirections) {
        return false;
    }

    @Override
    public GameEntity chooseSingleEntityForEffect(FCollectionView<? extends GameEntity> optionList, DelayedReveal delayedReveal, SpellAbility sa, String title, boolean isOptional, Player targetedPlayer) {
        return optionList.isEmpty() ? null : optionList.getFirst();
    }

    @Override
    public <T extends GameEntity> List<T> chooseEntitiesForEffect(FCollectionView<T> optionList, int min, int max, DelayedReveal delayedReveal, SpellAbility sa, String title, Player targetedPlayer) {
        List<T> result = new ArrayList<>();
        int count = Math.min(min, optionList.size());
        for (int i = 0; i < count; i++) {
            result.add(optionList.get(i));
        }
        return result;
    }

    @Override
    public SpellAbility chooseSingleSpellForEffect(List<SpellAbility> spells, SpellAbility sa, String title, Map<String, Object> params) {
        return spells.isEmpty() ? null : spells.get(0);
    }

    @Override
    public SpellAbility getAbilityToPlay(Card hostCard, List<SpellAbility> abilities, ITriggerEvent triggerEvent) {
        return null; // Don't play any abilities
    }

    @Override
    public void playSpellAbilityForFree(SpellAbility copySA, boolean mayChoseNewTargets) {
        // Don't play abilities in headless mode during initialization
    }

    @Override
    public void playSpellAbilityNoStack(SpellAbility effectSA, boolean canSetupTargets) {
        // Don't play abilities in headless mode during initialization
    }

    @Override
    public List<SpellAbility> chooseSpellAbilitiesForEffect(List<SpellAbility> spells, SpellAbility sa, String title, int num, Map<String, Object> params) {
        return new ArrayList<>();
    }

    @Override
    public boolean confirmAction(SpellAbility sa, PlayerActionConfirmMode mode, String message, Map<String, Object> params) {
        return false;
    }

    @Override
    public boolean confirmBidAction(SpellAbility sa, PlayerActionConfirmMode bidlife, String string, int bid, Player winner) {
        return false;
    }

    @Override
    public boolean confirmStaticApplication(Card hostCard, GameEntity affected, String logic, String message) {
        return false;
    }

    @Override
    public boolean confirmTrigger(SpellAbility sa, Map<String, String> triggerParams, boolean isMandatory) {
        return isMandatory;
    }

    @Override
    public Player chooseStartingPlayer(boolean isFirstGame) {
        // Randomly choose starting player
        List<Player> players = new ArrayList<>(getGame().getPlayers());
        return players.get(MyRandom.getRandom().nextInt(players.size()));
    }

    @Override
    public CardCollection orderBlockers(Card attacker, CardCollection blockers) {
        return blockers;
    }

    @Override
    public CardCollection orderBlocker(Card attacker, Card blocker, CardCollection oldBlockers) {
        return oldBlockers;
    }

    @Override
    public CardCollection orderAttackers(Card blocker, CardCollection attackers) {
        return attackers;
    }

    @Override
    public void reveal(CardCollectionView cards, ZoneType zone, Player owner, String messagePrefix) {
        // No-op
    }

    @Override
    public void reveal(List<CardView> cards, ZoneType zone, PlayerView owner, String messagePrefix) {
        // No-op
    }

    @Override
    public ImmutablePair<CardCollection, CardCollection> arrangeForScry(CardCollection topN) {
        return ImmutablePair.of(new CardCollection(), topN);
    }

    @Override
    public ImmutablePair<CardCollection, CardCollection> arrangeForSurveil(CardCollection topN) {
        return ImmutablePair.of(new CardCollection(), topN);
    }

    @Override
    public boolean willPutCardOnTop(Card c) {
        return true;
    }

    @Override
    public CardCollectionView orderMoveToZoneList(CardCollectionView cards, ZoneType destinationZone, Card destination, SpellAbility source) {
        return cards;
    }

    @Override
    public CardCollection chooseCardsToDiscardFrom(Player playerDiscard, SpellAbility sa, CardCollection validCards, int min, int max) {
        CardCollection result = new CardCollection();
        int count = Math.min(min, validCards.size());
        for (int i = 0; i < count; i++) {
            result.add(validCards.get(i));
        }
        return result;
    }

    @Override
    public CardCollectionView chooseCardsToDelve(int genericCost, CardCollection grave) {
        return new CardCollection();
    }

    @Override
    public CardCollectionView chooseCardsToRevealFromHand(int min, int max, CardCollectionView valid) {
        return new CardCollection();
    }

    @Override
    public boolean payManaOptional(Card card, Cost cost, SpellAbility sa, String prompt, ManaPaymentPurpose purpose) {
        return false;
    }

    @Override
    public List<SpellAbility> chooseSpellAbilityToPlay() {
        return null; // Don't play anything
    }

    @Override
    public CardCollectionView choiseSingleCardForZoneChange(ZoneType destination, List<ZoneType> origin, SpellAbility sa, CardCollectionView fetchList, DelayedReveal delayedReveal, String selectPrompt, boolean isOptional, Player decider) {
        return new CardCollection();
    }

    @Override
    public void notifyOfValue(SpellAbility saSource, GameObject realtedTarget, String value) {
        // No-op
    }

    @Override
    public Map<GameEntity, CounterType> chooseProliferation() {
        return new HashMap<>();
    }

    @Override
    public boolean chooseTargetsFor(SpellAbility currentAbility) {
        return false;
    }

    @Override
    public boolean chooseCardsPile(SpellAbility sa, CardCollectionView pile1, CardCollectionView pile2, String faceupOrFaceDown) {
        return MyRandom.getRandom().nextBoolean();
    }

    @Override
    public void revealAnte(String message, Multimap<Player, PaperCard> removedAnteCards) {
        // No-op
    }

    @Override
    public CardCollectionView chooseCardsForEffect(CardCollectionView sourceList, SpellAbility sa, String title, int min, int max, boolean isOptional, Map<String, Object> params) {
        CardCollection result = new CardCollection();
        int count = Math.min(min, sourceList.size());
        for (int i = 0; i < count; i++) {
            result.add(sourceList.get(i));
        }
        return result;
    }

    @Override
    public <T> List<T> chooseItems(List<T> choices, int min, int max, String title, boolean isOptional) {
        List<T> result = new ArrayList<>();
        int count = Math.min(min, choices.size());
        for (int i = 0; i < count; i++) {
            result.add(choices.get(i));
        }
        return result;
    }

    @Override
    public int chooseNumber(SpellAbility sa, String title, int min, int max) {
        return min;
    }

    @Override
    public int chooseNumber(SpellAbility sa, String title, List<Integer> options, Player relatedPlayer) {
        return options.isEmpty() ? 0 : options.get(0);
    }

    @Override
    public SpellAbility chooseSingleSpellForEffect(FCollectionView<SpellAbility> spells, SpellAbility sa, String title) {
        return spells.isEmpty() ? null : spells.getFirst();
    }

    @Override
    public Map<GameEntity, Integer> divideShield(Card effectSource, Map<GameEntity, Integer> affected, int shieldAmount) {
        return new HashMap<>();
    }

    @Override
    public Map<Byte, Integer> specifyManaCombo(SpellAbility sa, ColorSet colorSet, int manaAmount, boolean different) {
        return new HashMap<>();
    }

    @Override
    public byte chooseColor(String message, SpellAbility sa, ColorSet colors) {
        return colors.getColor();
    }

    @Override
    public ICardFace chooseSingleCardFace(SpellAbility sa, String message, Predicate<ICardFace> cpp, String name) {
        return null;
    }

    @Override
    public CounterType chooseCounterType(List<CounterType> options, SpellAbility sa, String prompt, Map<String, Object> params) {
        return options.isEmpty() ? null : options.get(0);
    }

    @Override
    public boolean confirmReplacementEffect(ReplacementEffect replacementEffect, SpellAbility effectSA, GameEntity affected, String question) {
        return false;
    }

    @Override
    public CardCollection chooseCardsForSplice(SpellAbility sa, List<Card> cards) {
        return new CardCollection();
    }

    @Override
    public String chooseProtectionType(String string, SpellAbility sa, List<String> choices) {
        return choices.isEmpty() ? "" : choices.get(0);
    }

    @Override
    public boolean payCostToPreventEffect(Cost cost, SpellAbility sa, boolean alreadyPaid, FCollectionView<Player> allPayers) {
        return false;
    }

    @Override
    public void orderAndPlaySimultaneousSa(List<SpellAbility> activePlayerSAs) {
        // No-op
    }

    @Override
    public void playTrigger(Card host, WrappedAbility wrapperAbility, boolean isMandatory) {
        // No-op
    }

    @Override
    public boolean playSaFromPlayEffect(SpellAbility tgtSA) {
        return false;
    }

    @Override
    public boolean chooseTargetsFor(SpellAbility currentAbility, SpellAbilityStackInstance si) {
        return false;
    }

    @Override
    public CardCollectionView chooseCardsForEffectMultiple(Map<String, CardCollectionView> validMap, SpellAbility sa, String title, int min, int max) {
        return new CardCollection();
    }

    @Override
    public TargetChoices chooseNewTargetsFor(SpellAbility ability) {
        return null;
    }

    @Override
    public boolean confirmMulliganScry(Player p) {
        return false;
    }

    @Override
    public void declareAttackers(Player attacker, Combat combat) {
        // No-op - don't declare any attackers
    }

    @Override
    public void declareBlockers(Player defender, Combat combat) {
        // No-op - don't declare any blockers
    }

    @Override
    public List<SpellAbility> chooseSpellAbilityToPlayFromPlayEffect(List<SpellAbility> sas) {
        return new ArrayList<>();
    }

    @Override
    public void playMoltenBirthEffect(Card host, int amount) {
        // No-op
    }

    @Override
    public void playChooseGenericEffect(Card host, List<String> options, Map<String, SpellAbility> saMap, int min, int max) {
        // No-op
    }

    @Override
    public Card chooseSingleCardForZoneChange(ZoneType destination, List<ZoneType> origin, SpellAbility sa, CardCollectionView fetchList, DelayedReveal delayedReveal, String selectPrompt, boolean isOptional, Player decider) {
        return null;
    }

    @Override
    public CardCollectionView chooseCardsForZoneChange(ZoneType destination, List<ZoneType> origin, SpellAbility sa, CardCollectionView fetchList, int min, int max, DelayedReveal delayedReveal, String selectPrompt, Player decider) {
        return new CardCollection();
    }

    @Override
    public List<Integer> diceRolls(int numRolls, int sides, SpellAbility sa, Player p, RollDiceEffect.ResultMode resultsMode) {
        List<Integer> results = new ArrayList<>();
        for (int i = 0; i < numRolls; i++) {
            results.add(MyRandom.getRandom().nextInt(sides) + 1);
        }
        return results;
    }

    @Override
    public boolean chooseFlipResult(SpellAbility sa, Player flipper, boolean[] results, boolean call) {
        return MyRandom.getRandom().nextBoolean();
    }

    @Override
    public Pair<SpellAbilityStackInstance, GameObject> chooseTarget(SpellAbility saSpellskite, List<Pair<SpellAbilityStackInstance, GameObject>> allTargets) {
        return allTargets.isEmpty() ? null : allTargets.get(0);
    }

    @Override
    public void notifyManaPool(PlayerView p) {
        // No-op
    }

    @Override
    public void updateButtons(Player viewer, String label1, String label2, boolean enable1, boolean enable2, boolean altState) {
        // No-op
    }

    @Override
    public void autoPassCancel() {
        // No-op
    }

    @Override
    public void awaitNextInput() {
        // No-op
    }

    @Override
    public void cancelAwaitNextInput() {
        // No-op
    }

    @Override
    public int chooseNumberForKeywordAction(SpellAbility sa, String titleFormatString, List<Integer> choices, Player relatedPlayer) {
        return choices.isEmpty() ? 0 : choices.get(0);
    }

    @Override
    public CardCollectionView chooseCardsForConvokeOrImprovise(SpellAbility sa, ManaCost manaCost, CardCollectionView untappedCards, boolean improvise) {
        return new CardCollection();
    }

    @Override
    public String chooseCardName(SpellAbility sa, Predicate<ICardFace> cpp, String valid, String message) {
        return "";
    }

    @Override
    public String chooseCardName(SpellAbility sa, List<ICardFace> faces, String message) {
        return faces.isEmpty() ? "" : faces.get(0).getName();
    }

    @Override
    public Card chooseProtectionShield(GameEntity entityBeingDamaged, List<String> options, Map<String, Card> choiceMap) {
        return choiceMap.isEmpty() ? null : choiceMap.values().iterator().next();
    }

    @Override
    public List<Card> chooseCardsForDiscardAndGainLife(int numCards, CardCollectionView validCards, boolean isOptional) {
        return new ArrayList<>();
    }

    @Override
    public List<Card> chooseCardsForDiscardOrCounter(SpellAbility effectSA, int min, int max, CardCollectionView hand, String payCardType, boolean isOptional) {
        return new ArrayList<>();
    }

    @Override
    public boolean payManaCost(ManaCost cost, CostPartMana costPartMana, SpellAbility sa, String prompt, ManaConversionMatrix matrix, boolean isActivatedSa) {
        return false;
    }

    @Override
    public Map<Card, ManaCostShard> chooseCardsForConvoke(SpellAbility sa, ManaCost manaCost, CardCollectionView untappedCards) {
        return new HashMap<>();
    }

    @Override
    public String chooseSomeType(String kindOfType, SpellAbility sa, List<String> validTypes, List<String> invalidTypes, boolean isOptional) {
        return validTypes.isEmpty() ? "" : validTypes.get(0);
    }

    @Override
    public Object vote(SpellAbility sa, String prompt, List<Object> options, ListMultimap<Object, Player> votes, Player forPlayer) {
        return options.isEmpty() ? null : options.get(0);
    }

    @Override
    public boolean confirmPayment(CostPart costPart, String question, SpellAbility sa) {
        return false;
    }

    @Override
    public ReplacementEffect chooseSingleReplacementEffect(String prompt, List<ReplacementEffect> possibleReplacers, Map<String, Object> runParams) {
        return possibleReplacers.isEmpty() ? null : possibleReplacers.get(0);
    }

    @Override
    public String chooseKeywordForPump(List<String> keywords, SpellAbility sa, String prompt, Card tgtCard) {
        return keywords.isEmpty() ? "" : keywords.get(0);
    }

    @Override
    public byte chooseColorAllowColorless(String message, Card card, ColorSet colors) {
        return colors.getColor();
    }

    @Override
    public Map<String, Object> chooseFromPossibilities(String message, Map<String, Object> possibleValues) {
        return new HashMap<>();
    }

    @Override
    public PaperCard chooseSinglePaperCard(SpellAbility sa, String message, Predicate<PaperCard> cpp, String name) {
        return null;
    }

    @Override
    public List<String> chooseColors(String message, SpellAbility sa, int min, int max, List<String> options) {
        List<String> result = new ArrayList<>();
        int count = Math.min(min, options.size());
        for (int i = 0; i < count; i++) {
            result.add(options.get(i));
        }
        return result;
    }

    @Override
    public CounterType chooseCounterType(List<CounterType> options, SpellAbility sa, String prompt, boolean isOptional, Map<String, Object> params) {
        return options.isEmpty() ? null : options.get(0);
    }

    @Override
    public boolean chooseBinary(SpellAbility sa, String question, BinaryChoiceType kindOfChoice, Map<String, Object> params) {
        if (kindOfChoice == BinaryChoiceType.PlayOrDraw) {
            return MyRandom.getRandom().nextBoolean();
        }
        return false;
    }

    @Override
    public List<AbilityBasedEffect> chooseAbilityBasedEffect(SpellAbility sa, int num, Map<String, List<AbilityBasedEffect>> choiceMap) {
        return new ArrayList<>();
    }

    @Override
    public void resetAtEndOfTurn() {
        // No-op
    }

    @Override
    public boolean payManaOptional(Card card, Cost cost, SpellAbility sa, String prompt, ManaPaymentPurpose purpose, Map<String, Object> params) {
        return false;
    }

    @Override
    public int announceXMana(SpellAbility saBeingCast) {
        return 0;
    }

    @Override
    public Map<CounterType, Integer> chooseCountersToRemove(SpellAbility sa, Map<CounterType, Integer> counterMap, int min, int max, String typesDescription) {
        return new HashMap<>();
    }

    @Override
    public int chooseXValue(SpellAbility sa, int max) {
        return 0;
    }

    @Override
    public List<StaticAbility> chooseStaticAbilities(String message, CardCollectionView pile, int num, List<Card> toShow, List<StaticAbility> chosen) {
        return new ArrayList<>();
    }

    @Override
    public boolean payManaFromPool(List<Mana> manaSpent, ManaCostBeingPaid cost, SpellAbility sa) {
        return false;
    }

    @Override
    public int chooseXValue(SpellAbility sa, String title, int max) {
        return 0;
    }

    @Override
    public KeywordInterface chooseKeyword(List<KeywordInterface> keywords) {
        return keywords.isEmpty() ? null : keywords.get(0);
    }

    @Override
    public boolean willPreventDamage(GameEntity gi, boolean isCombat, boolean isEffect, int damage, CardCollectionView possibleShields) {
        return false;
    }

    @Override
    public boolean isGuiPlayer() {
        return false;
    }

    @Override
    public void showErrorDialog(String message, String title) {
        // No-op
    }

    @Override
    public void showInfoDialog(String message, String title) {
        // No-op
    }

    @Override
    public void showMessageDialog(String message) {
        // No-op
    }
}
