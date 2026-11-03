// The projector font (Orbitron) has no Polish letters, so everything shown on the player screen is
// folded to plain ASCII: ą -> a, ł -> l, ... The data files can keep proper Polish spelling.

/** Removes diacritics: "Wiedźmin Łódź" -> "Wiedzmin Lodz". */
export function plain(value) {
    return String(value ?? '')
        .normalize('NFD')
        .replace(/[̀-ͯ]/g, '')
        .replace(/ł/g, 'l')
        .replace(/Ł/g, 'L');
}
