package com.example.staffmode.gui;

import com.example.staffmode.notes.StaffNotesManager;
import com.example.staffmode.staff.StaffModeManager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Handles Staff Notes and Moderation Notes.
 *
 * While writing a note:
 * - Player is frozen
 * - Writable book is forced into slot 5
 * - Cancel barrier is placed in slot 9
 * - Player can only switch between those two slots
 * - Player cannot move/drop the temporary items
 *
 * When the note is saved or cancelled:
 * - Temporary items are removed
 * - Player is unfrozen
 */
public class NotesGui implements Listener {

    private final JavaPlugin plugin;
    private final StaffNotesManager notesManager;
    private final StaffModeManager staffModeManager;

    private final NamespacedKey newNoteKey;
    private final NamespacedKey noteTypeKey;
    private final NamespacedKey cancelNoteKey;

    /**
     * Players currently writing a note.
     */
    private final Set<UUID> writingNote =
            new HashSet<>();

    /**
     * Slot 5 in the visible hotbar.
     *
     * Java counts:
     * 0 = slot 1
     * 4 = slot 5
     */
    private static final int NOTE_SLOT = 4;

    /**
     * Slot 9 in the visible hotbar.
     *
     * Java slot 8 = visible slot 9.
     */
    private static final int CANCEL_SLOT = 8;

    public NotesGui(
            JavaPlugin plugin,
            StaffNotesManager notesManager,
            StaffModeManager staffModeManager
    ) {

        this.plugin = plugin;
        this.notesManager = notesManager;
        this.staffModeManager = staffModeManager;

        this.newNoteKey =
                new NamespacedKey(
                        plugin,
                        "new_staff_note"
                );

        this.noteTypeKey =
                new NamespacedKey(
                        plugin,
                        "staff_note_type"
                );

        this.cancelNoteKey =
                new NamespacedKey(
                        plugin,
                        "cancel_staff_note"
                );
    }

    /**
     * Opens general staff notes.
     */
    public void openStaffNotes(Player player) {

        openNotesMenu(
                player,
                notesManager.getStaffNotes(),
                "Staff Notes",
                "staff"
        );
    }

    /**
     * Opens moderation notes.
     */
    public void openModNotes(Player player) {

        openNotesMenu(
                player,
                notesManager.getModNotes(),
                "Moderation Notes",
                "mod"
        );
    }

    /**
     * Builds a notes inventory.
     */
    private void openNotesMenu(
            Player player,
            YamlConfiguration config,
            String title,
            String noteType
    ) {

        Inventory menu =
                Bukkit.createInventory(
                        null,
                        54,
                        title
                );

        boolean canAdd;

        if (noteType.equals("mod")) {

            canAdd =
                    player.hasPermission(
                            "staffmode.notes.mod.add"
                    );

        } else {

            canAdd =
                    player.hasPermission(
                            "staffmode.notes.staff.add"
                    );
        }

        /**
         * Writable book for creating a new note.
         */
        if (canAdd) {

            menu.setItem(
                    49,
                    createNewNoteButton(noteType)
            );
        }

        int count =
                config.getInt(
                        "count",
                        0
                );

        int slot = 0;

        /**
         * Newest notes appear first.
         */
        for (
                int i = count;
                i >= 1 && slot < 45;
                i--
        ) {

            menu.setItem(
                    slot,
                    createPastNoteBook(
                            config,
                            i,
                            noteType
                    )
            );

            slot++;
        }

        /**
         * Display this when there are no notes.
         */
        if (count <= 0) {

            ItemStack empty =
                    new ItemStack(
                            Material.BARRIER
                    );

            ItemMeta meta =
                    empty.getItemMeta();

            meta.setDisplayName(
                    ChatColor.YELLOW
                            + "No notes recorded yet"
            );

            empty.setItemMeta(meta);

            menu.setItem(
                    22,
                    empty
            );
        }

        player.openInventory(menu);
    }

    /**
     * Creates the New Note button.
     */
    private ItemStack createNewNoteButton(
            String noteType
    ) {

        ItemStack book =
                new ItemStack(
                        Material.WRITABLE_BOOK
                );

        BookMeta meta =
                (BookMeta) book.getItemMeta();

        if (noteType.equals("mod")) {

            meta.setDisplayName(
                    ChatColor.RED
                            + "New Moderation Note"
            );

            meta.setLore(
                    List.of(
                            ChatColor.GRAY
                                    + "Click to write a moderation note."
                    )
            );

        } else {

            meta.setDisplayName(
                    ChatColor.GREEN
                            + "New Staff Note"
            );

            meta.setLore(
                    List.of(
                            ChatColor.GRAY
                                    + "Click to write a staff progress note."
                    )
            );
        }

        meta
                .getPersistentDataContainer()
                .set(
                        newNoteKey,
                        PersistentDataType.BYTE,
                        (byte) 1
                );

        meta
                .getPersistentDataContainer()
                .set(
                        noteTypeKey,
                        PersistentDataType.STRING,
                        noteType
                );

        book.setItemMeta(meta);

        return book;
    }

    /**
     * Creates a signed/read-only book
     * representing a saved note.
     */
    private ItemStack createPastNoteBook(
            YamlConfiguration config,
            int id,
            String noteType
    ) {

        String path =
                "entries." + id;

        String staff =
                config.getString(
                        path + ".staff",
                        "Unknown"
                );

        String world =
                config.getString(
                        path + ".world",
                        "Unknown"
                );

        String text =
                config.getString(
                        path + ".text",
                        "No note"
                );

        String date =
                config.getString(
                        path + ".date",
                        "Unknown"
                );

        ItemStack book =
                new ItemStack(
                        Material.WRITTEN_BOOK
                );

        BookMeta meta =
                (BookMeta) book.getItemMeta();

        if (noteType.equals("mod")) {

            meta.setTitle(
                    "Mod Note #" + id
            );

        } else {

            meta.setTitle(
                    "Staff Note #" + id
            );
        }

        meta.setAuthor(staff);

        meta.setDisplayName(
                ChatColor.AQUA
                        + "#"
                        + id
                        + " - "
                        + staff
        );

        meta.setLore(
                List.of(
                        ChatColor.GRAY
                                + "Date: "
                                + ChatColor.WHITE
                                + date,

                        ChatColor.GRAY
                                + "World: "
                                + ChatColor.WHITE
                                + world,

                        "",

                        ChatColor.YELLOW
                                + "Click to read"
                )
        );

        meta.setPages(
                createBookPages(
                        staff,
                        world,
                        date,
                        text
                )
        );

        book.setItemMeta(meta);

        return book;
    }

    /**
     * Handles clicks inside the notes GUIs.
     */
    @EventHandler
    public void onNotesClick(
            InventoryClickEvent event
    ) {

        if (!(event.getWhoClicked()
                instanceof Player player)) {

            return;
        }

        /**
         * Prevent inventory movement while
         * the player is writing a note.
         */
        if (writingNote.contains(
                player.getUniqueId()
        )) {

            event.setCancelled(true);
            return;
        }

        String title =
                event.getView().getTitle();

        if (!title.equals("Staff Notes")
                && !title.equals("Moderation Notes")) {

            return;
        }

        event.setCancelled(true);

        ItemStack clicked =
                event.getCurrentItem();

        if (clicked == null
                || clicked.getType()
                == Material.AIR) {

            return;
        }

        /**
         * NEW NOTE
         */
        if (clicked.getType()
                == Material.WRITABLE_BOOK) {

            ItemMeta meta =
                    clicked.getItemMeta();

            if (meta == null) {
                return;
            }

            if (!meta
                    .getPersistentDataContainer()
                    .has(
                            newNoteKey,
                            PersistentDataType.BYTE
                    )) {

                return;
            }

            String noteType =
                    meta
                            .getPersistentDataContainer()
                            .get(
                                    noteTypeKey,
                                    PersistentDataType.STRING
                            );

            if (noteType == null) {
                return;
            }

            /**
             * Permission check.
             */
            if (noteType.equals("mod")) {

                if (!player.hasPermission(
                        "staffmode.notes.mod.add"
                )) {

                    player.sendMessage(
                            ChatColor.RED
                                    + "You cannot add moderation notes."
                    );

                    return;
                }

            } else {

                if (!player.hasPermission(
                        "staffmode.notes.staff.add"
                )) {

                    player.sendMessage(
                            ChatColor.RED
                                    + "You cannot add staff notes."
                    );

                    return;
                }
            }

            /**
             * Require staff mode.
             */
            if (!staffModeManager
                    .isInStaffMode(player)) {

                player.sendMessage(
                        ChatColor.RED
                                + "You must be in staff mode to create a note."
                );

                return;
            }

            startWritingNote(
                    player,
                    noteType
            );

            return;
        }

        /**
         * READ SAVED NOTE
         */
        if (clicked.getType()
                == Material.WRITTEN_BOOK) {

            ItemStack book =
                    clicked.clone();

            player.closeInventory();

            Bukkit
                    .getScheduler()
                    .runTaskLater(
                            plugin,
                            () -> player.openBook(book),
                            1L
                    );
        }
    }

    /**
     * Starts the note-writing process.
     */
    private void startWritingNote(
            Player player,
            String noteType
    ) {

        writingNote.add(
                player.getUniqueId()
        );

        ItemStack writingBook =
                createWritingBook(
                        noteType
                );

        ItemStack cancelBarrier =
                createCancelItem();

        player.closeInventory();

        /**
         * Put writable book in slot 5.
         */
        player
                .getInventory()
                .setItem(
                        NOTE_SLOT,
                        writingBook
                );

        /**
         * Put cancel barrier in slot 9.
         */
        player
                .getInventory()
                .setItem(
                        CANCEL_SLOT,
                        cancelBarrier
                );

        /**
         * Force selected slot to the writable book.
         */
        player
                .getInventory()
                .setHeldItemSlot(
                        NOTE_SLOT
                );

        player.updateInventory();

        player.sendMessage(
                ChatColor.YELLOW
                        + "You are now writing a staff note."
        );

        player.sendMessage(
                ChatColor.GRAY
                        + "Right-click the book to write your note."
        );

        player.sendMessage(
                ChatColor.GRAY
                        + "Press Done when finished."
        );

        player.sendMessage(
                ChatColor.RED
                        + "Use the barrier in slot 9 to cancel."
        );
    }

    /**
     * Creates the temporary editable book.
     */
    private ItemStack createWritingBook(
            String noteType
    ) {

        ItemStack book =
                new ItemStack(
                        Material.WRITABLE_BOOK
                );

        BookMeta meta =
                (BookMeta) book.getItemMeta();

        if (noteType.equals("mod")) {

            meta.setDisplayName(
                    ChatColor.RED
                            + "New Moderation Note"
            );

        } else {

            meta.setDisplayName(
                    ChatColor.GREEN
                            + "New Staff Note"
            );
        }

        meta.addPage("");

        meta
                .getPersistentDataContainer()
                .set(
                        newNoteKey,
                        PersistentDataType.BYTE,
                        (byte) 1
                );

        meta
                .getPersistentDataContainer()
                .set(
                        noteTypeKey,
                        PersistentDataType.STRING,
                        noteType
                );

        book.setItemMeta(meta);

        return book;
    }

    /**
     * Creates the cancel barrier.
     */
    private ItemStack createCancelItem() {

        ItemStack barrier =
                new ItemStack(
                        Material.BARRIER
                );

        ItemMeta meta =
                barrier.getItemMeta();

        meta.setDisplayName(
                ChatColor.RED
                        + "Cancel Note"
        );

        meta.setLore(
                List.of(
                        ChatColor.GRAY
                                + "Right-click to cancel this note."
                )
        );

        meta
                .getPersistentDataContainer()
                .set(
                        cancelNoteKey,
                        PersistentDataType.BYTE,
                        (byte) 1
                );

        barrier.setItemMeta(meta);

        return barrier;
    }

    /**
     * Freezes player movement while
     * they are writing a note.
     *
     * They can still look around.
     */
    @EventHandler
    public void onMove(
            PlayerMoveEvent event
    ) {

        Player player =
                event.getPlayer();

        if (!writingNote.contains(
                player.getUniqueId()
        )) {

            return;
        }

        if (event.getTo() == null) {
            return;
        }

        Location from =
                event.getFrom();

        Location to =
                event.getTo();

        if (from.getX() != to.getX()
                || from.getY() != to.getY()
                || from.getZ() != to.getZ()) {

            Location frozen =
                    from.clone();

            /**
             * Allow the player to move their camera.
             */
            frozen.setYaw(
                    to.getYaw()
            );

            frozen.setPitch(
                    to.getPitch()
            );

            event.setTo(
                    frozen
            );
        }
    }

    /**
     * Only lets the player select:
     *
     * slot 5 = book
     * slot 9 = cancel barrier
     */
    @EventHandler
    public void onHeldItemChange(
            PlayerItemHeldEvent event
    ) {

        Player player =
                event.getPlayer();

        if (!writingNote.contains(
                player.getUniqueId()
        )) {

            return;
        }

        if (event.getNewSlot() != NOTE_SLOT
                && event.getNewSlot()
                != CANCEL_SLOT) {

            event.setCancelled(true);

            player
                    .getInventory()
                    .setHeldItemSlot(
                            NOTE_SLOT
                    );

            player.sendMessage(
                    ChatColor.RED
                            + "Finish or cancel your note first."
            );
        }
    }

    /**
     * Handles the cancel barrier.
     */
    @EventHandler
    public void onInteract(
            PlayerInteractEvent event
    ) {

        Player player =
                event.getPlayer();

        if (!writingNote.contains(
                player.getUniqueId()
        )) {

            return;
        }

        ItemStack item =
                player
                        .getInventory()
                        .getItemInMainHand();

        if (!isCancelItem(item)) {
            return;
        }

        event.setCancelled(true);

        cancelWritingNote(
                player
        );
    }

    /**
     * Prevents dropping temporary items.
     */
    @EventHandler
    public void onDrop(
            PlayerDropItemEvent event
    ) {

        Player player =
                event.getPlayer();

        if (!writingNote.contains(
                player.getUniqueId()
        )) {

            return;
        }

        event.setCancelled(true);

        player.sendMessage(
                ChatColor.RED
                        + "Finish or cancel your note first."
        );
    }

    /**
     * Prevent inventory dragging.
     */
    @EventHandler
    public void onInventoryDrag(
            InventoryDragEvent event
    ) {

        if (!(event.getWhoClicked()
                instanceof Player player)) {

            return;
        }

        if (writingNote.contains(
                player.getUniqueId()
        )) {

            event.setCancelled(true);
        }
    }

    /**
     * Saves the note when the player
     * presses Done in the book editor.
     */
    @EventHandler
    public void onBookEdit(
            PlayerEditBookEvent event
    ) {

        Player player =
                event.getPlayer();

        if (!writingNote.contains(
                player.getUniqueId()
        )) {

            return;
        }

        BookMeta previous =
                event.getPreviousBookMeta();

        if (!previous
                .getPersistentDataContainer()
                .has(
                        newNoteKey,
                        PersistentDataType.BYTE
                )) {

            return;
        }

        String noteType =
                previous
                        .getPersistentDataContainer()
                        .get(
                                noteTypeKey,
                                PersistentDataType.STRING
                        );

        if (noteType == null) {
            return;
        }

        BookMeta updated =
                event.getNewBookMeta();

        StringBuilder note =
                new StringBuilder();

        /**
         * Combine every page into one saved note.
         */
        for (String page :
                updated.getPages()) {

            if (page == null
                    || page.isBlank()) {

                continue;
            }

            if (!note.isEmpty()) {

                note.append(
                        "\n\n"
                );
            }

            note.append(
                    page.trim()
            );
        }

        /**
         * Do not accept an empty note.
         */
        if (note.toString().isBlank()) {

            event.setCancelled(true);

            player.sendMessage(
                    ChatColor.RED
                            + "Your note cannot be empty."
            );

            Bukkit
                    .getScheduler()
                    .runTaskLater(
                            plugin,
                            () -> forceWritingItems(player),
                            1L
                    );

            return;
        }

        /**
         * Save moderation note.
         */
        if (noteType.equals("mod")) {

            if (!player.hasPermission(
                    "staffmode.notes.mod.add"
            )) {

                event.setCancelled(true);

                cancelWritingNote(player);

                return;
            }

            notesManager.addModNote(
                    player,
                    note.toString()
            );

            player.sendMessage(
                    ChatColor.GREEN
                            + "Moderation note saved."
            );

        } else {

            /**
             * Save general staff note.
             */
            if (!player.hasPermission(
                    "staffmode.notes.staff.add"
            )) {

                event.setCancelled(true);

                cancelWritingNote(player);

                return;
            }

            notesManager.addStaffNote(
                    player,
                    note.toString()
            );

            player.sendMessage(
                    ChatColor.GREEN
                            + "Staff note saved."
            );
        }

        /**
         * Stop Minecraft from retaining
         * the temporary book.
         */
        event.setCancelled(true);

        finishWritingNote(
                player
        );
    }

    /**
     * Makes sure both temporary items
     * stay where they belong.
     */
    private void forceWritingItems(
            Player player
    ) {

        if (!writingNote.contains(
                player.getUniqueId()
        )) {

            return;
        }

        ItemStack book =
                player
                        .getInventory()
                        .getItem(
                                NOTE_SLOT
                        );

        if (!isPluginNoteBook(book)) {

            player
                    .getInventory()
                    .setItem(
                            NOTE_SLOT,
                            createWritingBook(
                                    "staff"
                            )
                    );
        }

        ItemStack cancel =
                player
                        .getInventory()
                        .getItem(
                                CANCEL_SLOT
                        );

        if (!isCancelItem(cancel)) {

            player
                    .getInventory()
                    .setItem(
                            CANCEL_SLOT,
                            createCancelItem()
                    );
        }

        player
                .getInventory()
                .setHeldItemSlot(
                        NOTE_SLOT
                );

        player.updateInventory();
    }

    /**
     * Checks whether an item is our
     * temporary writing book.
     */
    private boolean isPluginNoteBook(
            ItemStack item
    ) {

        if (item == null
                || item.getType()
                != Material.WRITABLE_BOOK) {

            return false;
        }

        ItemMeta meta =
                item.getItemMeta();

        if (meta == null) {
            return false;
        }

        return meta
                .getPersistentDataContainer()
                .has(
                        newNoteKey,
                        PersistentDataType.BYTE
                );
    }

    /**
     * Checks whether an item is our
     * cancel barrier.
     */
    private boolean isCancelItem(
            ItemStack item
    ) {

        if (item == null
                || item.getType()
                != Material.BARRIER) {

            return false;
        }

        ItemMeta meta =
                item.getItemMeta();

        if (meta == null) {
            return false;
        }

        return meta
                .getPersistentDataContainer()
                .has(
                        cancelNoteKey,
                        PersistentDataType.BYTE
                );
    }

    /**
     * Successfully finishes note writing.
     */
    private void finishWritingNote(
            Player player
    ) {

        writingNote.remove(
                player.getUniqueId()
        );

        Bukkit
                .getScheduler()
                .runTaskLater(
                        plugin,
                        () -> {

                            removeTemporaryItems(
                                    player
                            );

                            player.updateInventory();

                        },
                        1L
                );
    }

    /**
     * Cancels note writing.
     */
    private void cancelWritingNote(
            Player player
    ) {

        writingNote.remove(
                player.getUniqueId()
        );

        removeTemporaryItems(
                player
        );

        player.updateInventory();

        player.sendMessage(
                ChatColor.RED
                        + "Note cancelled."
        );
    }

    /**
     * Removes both the writable book
     * and the cancel barrier.
     */
    private void removeTemporaryItems(
            Player player
    ) {

        ItemStack book =
                player
                        .getInventory()
                        .getItem(
                                NOTE_SLOT
                        );

        if (isPluginNoteBook(book)) {

            player
                    .getInventory()
                    .setItem(
                            NOTE_SLOT,
                            null
                    );
        }

        ItemStack cancel =
                player
                        .getInventory()
                        .getItem(
                                CANCEL_SLOT
                        );

        if (isCancelItem(cancel)) {

            player
                    .getInventory()
                    .setItem(
                            CANCEL_SLOT,
                            null
                    );
        }
    }

    /**
     * Cleans up if a player leaves
     * while writing a note.
     */
    @EventHandler
    public void onQuit(
            PlayerQuitEvent event
    ) {

        Player player =
                event.getPlayer();

        if (!writingNote.contains(
                player.getUniqueId()
        )) {

            return;
        }

        writingNote.remove(
                player.getUniqueId()
        );

        removeTemporaryItems(
                player
        );
    }

    /**
     * Creates readable pages for
     * old signed books.
     */
    private List<String> createBookPages(
            String staff,
            String world,
            String date,
            String text
    ) {

        List<String> pages =
                new ArrayList<>();

        String header =
                "Staff: "
                        + staff
                        + "\n"
                        + "Date: "
                        + date
                        + "\n"
                        + "World: "
                        + world
                        + "\n\n";

        int maxCharacters =
                220;

        String remaining =
                text;

        boolean firstPage =
                true;

        while (!remaining.isEmpty()) {

            int length =
                    Math.min(
                            maxCharacters,
                            remaining.length()
                    );

            /**
             * Try to avoid splitting words.
             */
            if (length < remaining.length()) {

                int space =
                        remaining.lastIndexOf(
                                ' ',
                                length
                        );

                if (space > 50) {
                    length = space;
                }
            }

            String pageText =
                    remaining
                            .substring(
                                    0,
                                    length
                            )
                            .trim();

            remaining =
                    remaining
                            .substring(length)
                            .trim();

            if (firstPage) {

                pages.add(
                        header
                                + pageText
                );

                firstPage = false;

            } else {

                pages.add(
                        pageText
                );
            }
        }

        if (pages.isEmpty()) {

            pages.add(
                    header
            );
        }

        return pages;
    }
}