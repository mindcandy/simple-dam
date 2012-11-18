/* shared javascript */


/**
 * add a hashCode to String so we can hash paths
 */
String.prototype.hashCode = function(){
    var hash = 0, i, ch;
    if (this.length == 0) return hash;
    for (i = 0; i < this.length; i++) {
        ch = this.charCodeAt(i);
        hash = ((hash<<5)-hash)+ch;
        hash = hash & hash; // Convert to 32bit integer
    }
    return hash;
};
